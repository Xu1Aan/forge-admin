package com.mdframe.forge.plugin.system.external.weaver;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.external.weaver.model.ExternalOrg;
import com.mdframe.forge.plugin.system.external.weaver.model.ExternalUser;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 泛微全量拉取 + 差分同步
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeaverSyncService {
    private static final String PLATFORM = WeaverClient.PLATFORM;

    private final WeaverClient weaverClient;
    private final WeaverProperties properties;

    private final SysExternalSyncBatchMapper batchMapper;
    private final SysExternalOrgMapMapper orgMapMapper;
    private final SysExternalUserMapMapper userMapMapper;

    private final SysOrgMapper sysOrgMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserOrgMapper sysUserOrgMapper;

    private final WeaverSyncTransactionHelper transactionHelper;
    private final WeaverSyncAsyncExecutor weaverAsyncExecutor;

    private WeaverSyncService self;

    @Autowired
    @Lazy
    public void setSelf(WeaverSyncService self) {
        this.self = self;
    }

    /**
     * 手动触发：仅提交批次并立即返回；实际拉取与写库在后台线程执行。结果请查批次表或轮询同接口分页。
     */
    public WeaverSyncResult startAsyncWeaverSync() {
        Long tenantId = properties.getTenantId();
        if (tenantId == null) tenantId = 1L;

        // 并发控制：同一租户同一平台若已有 running 批次，直接返回该批次，避免重复并行拉取
        SysExternalSyncBatch running = batchMapper.selectOne(new LambdaQueryWrapper<SysExternalSyncBatch>()
                .eq(SysExternalSyncBatch::getTenantId, tenantId)
                .eq(SysExternalSyncBatch::getPlatform, PLATFORM)
                .eq(SysExternalSyncBatch::getStatus, "running")
                .orderByDesc(SysExternalSyncBatch::getStartedAt)
                .last("limit 1"));
        if (running != null) {
            WeaverSyncResult r = new WeaverSyncResult();
            r.setBatchId(running.getId());
            r.setStatus("running");
            return r;
        }

        SysExternalSyncBatch b = transactionHelper.insertRunningBatch("manual");
        weaverAsyncExecutor.runAsyncWeaverSync(b.getId());
        WeaverSyncResult r = new WeaverSyncResult();
        r.setBatchId(b.getId());
        r.setStatus("running");
        return r;
    }

    /**
     * 后台任务入口：在独立事务中执行同步，失败时另开事务将批次标为 failed。
     */
    public void executeWeaverSyncByIdAfterSubmit(Long batchId) {
        try {
            self.runSyncWorkTransactional(batchId);
        } catch (Exception e) {
            log.error("Weaver 同步失败 batchId={}", batchId, e);
            try {
                transactionHelper.markBatchFailed(batchId, e.getMessage());
            } catch (Exception ex) {
                log.error("Weaver 无法写入失败状态 batchId={}", batchId, ex);
            }
        }
    }

    /**
     * 定时任务等同路径：整段仍在一个事务内，失败时整批与写库回滚（与仅插入 running 的异步模式不同）。
     */
    @Transactional(rollbackFor = Exception.class)
    public WeaverSyncResult syncFull(String triggerType) {
        Long tenantId = properties.getTenantId();
        if (tenantId == null) tenantId = 1L;

        SysExternalSyncBatch batch = new SysExternalSyncBatch();
        batch.setTenantId(tenantId);
        batch.setPlatform(PLATFORM);
        batch.setTriggerType(StringUtils.defaultIfBlank(triggerType, "schedule"));
        batch.setStatus("running");
        batch.setStartedAt(LocalDateTime.now());
        batchMapper.insert(batch);

        return self.runSyncWorkTransactional(batch.getId());
    }

    /**
     * 核心同步逻辑。由 {@link #syncFull} 在同事务内调用，或由 {@link #executeWeaverSyncByIdAfterSubmit} 在独立事务中调用。
     */
    @Transactional(rollbackFor = Exception.class)
    public WeaverSyncResult runSyncWorkTransactional(Long batchId) {
        SysExternalSyncBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalStateException("同步批次不存在: " + batchId);
        }
        if (!"running".equals(batch.getStatus())) {
            log.info("Weaver 同步跳过，批次已非 running: batchId={} status={}", batchId, batch.getStatus());
            WeaverSyncResult r = new WeaverSyncResult();
            r.setBatchId(batchId);
            r.setStatus(batch.getStatus());
            return r;
        }
        Long tenantId = batch.getTenantId();
        if (tenantId == null) {
            tenantId = 1L;
        }
        final Long tenant = tenantId;

        WeaverClient.Snapshot snapshot = weaverClient.fetchSnapshot();
        batch.setRawSnapshotHash(snapshot.getRawHash());

        List<ExternalOrg> orgs = snapshot.getOrgs() != null ? snapshot.getOrgs() : List.of();
        List<ExternalUser> users = snapshot.getUsers() != null ? new ArrayList<>(snapshot.getUsers()) : new ArrayList<>();
        batch.setFetchedOrgCount(orgs.size());
        batch.setFetchedUserCount(users.size());

        SyncCounters counters = new SyncCounters();
        StringBuilder syncNotes = new StringBuilder();
        if (snapshot.getFlatSkippedNoExternalUserId() > 0) {
            int n = snapshot.getFlatSkippedNoExternalUserId();
            counters.skipped += n;
            syncNotes.append("无工号且未回退 resource_id 的 user 行:").append(n).append(";");
        }
        users = dedupeUsersByExternalId(users, counters, syncNotes);

        Map<String, Long> externalOrgIdToLocalOrgId = upsertOrgs(tenant, batch.getId(), orgs, counters);
        upsertUsers(tenant, batch.getId(), users, externalOrgIdToLocalOrgId, counters);

        reconcileMissingUsers(tenant, batch.getId(), counters);

        batch.setInsertedCount(counters.inserted);
        batch.setUpdatedCount(counters.updated);
        batch.setDisabledCount(counters.disabled);
        batch.setSkippedCount(counters.skipped);
        if (syncNotes.length() > 0) {
            batch.setErrorMessage(syncNotes.toString());
            batch.setStatus("partial");
        } else {
            batch.setStatus("success");
        }
        batch.setEndedAt(LocalDateTime.now());
        batchMapper.updateById(batch);

        WeaverSyncResult result = new WeaverSyncResult();
        result.setBatchId(batch.getId());
        result.setStatus(batch.getStatus());
        result.setFetchedOrgCount(batch.getFetchedOrgCount());
        result.setFetchedUserCount(batch.getFetchedUserCount());
        result.setInsertedCount(counters.inserted);
        result.setUpdatedCount(counters.updated);
        result.setDisabledCount(counters.disabled);
        result.setSkippedCount(counters.skipped);
        return result;
    }

    /**
     * 同一批次内重复 externalUserId（工号）只保留首次出现，其余计入 skipped
     */
    private List<ExternalUser> dedupeUsersByExternalId(List<ExternalUser> users, SyncCounters counters, StringBuilder syncNotes) {
        if (users == null || users.isEmpty()) {
            return users;
        }
        Map<String, ExternalUser> first = new LinkedHashMap<>();
        int dup = 0;
        for (ExternalUser u : users) {
            String key = u.getExternalUserId();
            if (StringUtils.isBlank(key)) {
                continue;
            }
            if (first.containsKey(key)) {
                dup++;
                counters.skipped++;
            } else {
                first.put(key, u);
            }
        }
        if (dup > 0) {
            syncNotes.append("批次内重复工号跳过:").append(dup).append(";");
        }
        return new ArrayList<>(first.values());
    }

    private Map<String, Long> upsertOrgs(Long tenantId, Long batchId, List<ExternalOrg> orgs, SyncCounters counters) {
        if (orgs == null || orgs.isEmpty()) return new HashMap<>();

        // 根公司节点：所有顶级部门统一挂到公司下
        Long rootCompanyOrgId = ensureRootCompanyOrg(tenantId);

        // preload mapping
        List<SysExternalOrgMap> existingMaps = orgMapMapper.selectList(new LambdaQueryWrapper<SysExternalOrgMap>()
                .eq(SysExternalOrgMap::getTenantId, tenantId)
                .eq(SysExternalOrgMap::getPlatform, PLATFORM));
        Map<String, SysExternalOrgMap> byExternalId = new HashMap<>();
        for (SysExternalOrgMap m : existingMaps) {
            byExternalId.put(m.getExternalOrgId(), m);
        }

        Map<String, Long> externalOrgIdToLocalOrgId = new HashMap<>();
        for (ExternalOrg ext : orgs) {
            if (StringUtils.isBlank(ext.getExternalOrgId())) continue;
            SysExternalOrgMap map = byExternalId.get(ext.getExternalOrgId());

            if (map == null) {
                // create org（租户内 org_name 唯一，泛微不同路径可同名，需去重）
                SysOrg org = new SysOrg();
                org.setTenantId(tenantId);
                String baseName = StringUtils.defaultIfBlank(ext.getName(), ext.getExternalOrgId());
                org.setOrgName(resolveUniqueOrgName(tenantId, baseName, ext.getExternalOrgId(), null));
                org.setOrgType(2);
                org.setOrgStatus(parseOrgStatus(ext.getStatus()));
                org.setSort(ext.getSort() != null ? ext.getSort() : 0);
                org.setParentId(0L); // parent/ancestors 后续二次修正
                org.setAncestors("0");
                sysOrgMapper.insert(org);

                SysExternalOrgMap newMap = new SysExternalOrgMap();
                newMap.setTenantId(tenantId);
                newMap.setPlatform(PLATFORM);
                newMap.setExternalOrgId(ext.getExternalOrgId());
                newMap.setExternalParentId(ext.getExternalParentId());
                newMap.setOrgId(org.getId());
                newMap.setNameSnapshot(ext.getName());
                newMap.setStatusSnapshot(ext.getStatus());
                newMap.setLastExternalUpdateTime(ext.getUpdateTime());
                newMap.setLastSeenBatchId(batchId);
                orgMapMapper.insert(newMap);

                counters.inserted++;
                externalOrgIdToLocalOrgId.put(ext.getExternalOrgId(), org.getId());
                byExternalId.put(ext.getExternalOrgId(), newMap);
            } else {
                externalOrgIdToLocalOrgId.put(ext.getExternalOrgId(), map.getOrgId());

                boolean needUpdate = isNewer(ext.getUpdateTime(), map.getLastExternalUpdateTime())
                        || !Objects.equals(StringUtils.trimToEmpty(ext.getName()), StringUtils.trimToEmpty(map.getNameSnapshot()))
                        || !Objects.equals(StringUtils.trimToEmpty(ext.getStatus()), StringUtils.trimToEmpty(map.getStatusSnapshot()))
                        || !Objects.equals(StringUtils.trimToEmpty(ext.getExternalParentId()), StringUtils.trimToEmpty(map.getExternalParentId()));

                map.setLastSeenBatchId(batchId);
                if (!needUpdate) {
                    orgMapMapper.updateById(map);
                    counters.skipped++;
                    continue;
                }

                SysOrg org = sysOrgMapper.selectById(map.getOrgId());
                if (org != null) {
                    String baseName = StringUtils.defaultIfBlank(ext.getName(), org.getOrgName());
                    org.setOrgName(resolveUniqueOrgName(tenantId, baseName, ext.getExternalOrgId(), org.getId()));
                    org.setOrgStatus(parseOrgStatus(ext.getStatus()));
                    if (ext.getSort() != null) {
                        org.setSort(ext.getSort());
                    }
                    sysOrgMapper.updateById(org);
                }

                map.setExternalParentId(ext.getExternalParentId());
                map.setNameSnapshot(ext.getName());
                map.setStatusSnapshot(ext.getStatus());
                map.setLastExternalUpdateTime(ext.getUpdateTime());
                orgMapMapper.updateById(map);
                counters.updated++;
            }
        }

        // 修正 parentId/ancestors：需要确保父组织已存在
        for (ExternalOrg ext : orgs) {
            if (StringUtils.isBlank(ext.getExternalOrgId())) continue;
            SysExternalOrgMap map = byExternalId.get(ext.getExternalOrgId());
            if (map == null) continue;
            SysOrg org = sysOrgMapper.selectById(map.getOrgId());
            if (org == null) continue;

            Long parentLocalId = rootCompanyOrgId != null ? rootCompanyOrgId : 0L;
            if (StringUtils.isNotBlank(ext.getExternalParentId())) {
                SysExternalOrgMap parentMap = byExternalId.get(ext.getExternalParentId());
                if (parentMap != null) parentLocalId = parentMap.getOrgId();
            }
            if (parentLocalId == null) parentLocalId = 0L;

            if (!Objects.equals(org.getParentId(), parentLocalId)) {
                org.setParentId(parentLocalId);
                org.setAncestors(buildAncestors(parentLocalId));
                sysOrgMapper.updateById(org);
            } else if (StringUtils.isBlank(org.getAncestors())) {
                org.setAncestors(buildAncestors(parentLocalId));
                sysOrgMapper.updateById(org);
            }
        }

        return externalOrgIdToLocalOrgId;
    }

    private Long ensureRootCompanyOrg(Long tenantId) {
        String name = StringUtils.trimToEmpty(properties.getRootCompanyName());
        if (StringUtils.isBlank(name)) {
            name = "XX公司";
        }
        // 尽量复用现有 company 组织：orgType=1 且同名
        SysOrg existing = sysOrgMapper.selectOne(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, tenantId)
                .eq(SysOrg::getOrgType, 1)
                .eq(SysOrg::getOrgName, name)
                .last("limit 1"));
        if (existing != null) {
            // 确保它是顶级
            if (existing.getParentId() == null || existing.getParentId() != 0L) {
                existing.setParentId(0L);
                existing.setAncestors("0");
                sysOrgMapper.updateById(existing);
            } else if (StringUtils.isBlank(existing.getAncestors())) {
                existing.setAncestors("0");
                sysOrgMapper.updateById(existing);
            }
            return existing.getId();
        }

        SysOrg org = new SysOrg();
        org.setTenantId(tenantId);
        org.setOrgName(resolveUniqueOrgName(tenantId, name, "weaver_root_company", null));
        org.setOrgType(1);
        org.setOrgStatus(1);
        org.setSort(0);
        org.setParentId(0L);
        org.setAncestors("0");
        sysOrgMapper.insert(org);
        return org.getId();
    }

    /**
     * 满足 uk_tenant_org_name：同租户下 org_name 唯一。泛微侧不同 department 可能同名，冲突时在名称后附加「（外部id）」。
     */
    private String resolveUniqueOrgName(Long tenantId, String baseName, String externalOrgId, Long existingLocalOrgId) {
        String name = StringUtils.trimToEmpty(baseName);
        if (StringUtils.isBlank(name)) {
            name = StringUtils.defaultIfBlank(externalOrgId, "weaver-org");
        }
        if (isOrgNameAvailable(tenantId, name, existingLocalOrgId)) {
            return name;
        }
        String suffix = StringUtils.isNotBlank(externalOrgId) ? externalOrgId : UUID.randomUUID().toString();
        String disambiguated = name + "（" + suffix + "）";
        int n = 0;
        while (!isOrgNameAvailable(tenantId, disambiguated, existingLocalOrgId) && n < 32) {
            n++;
            disambiguated = name + "（" + suffix + "-" + n + "）";
        }
        return disambiguated;
    }

    private boolean isOrgNameAvailable(Long tenantId, String orgName, Long excludeLocalOrgId) {
        if (StringUtils.isBlank(orgName)) {
            return false;
        }
        LambdaQueryWrapper<SysOrg> q = new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, tenantId)
                .eq(SysOrg::getOrgName, orgName);
        if (excludeLocalOrgId != null) {
            q.ne(SysOrg::getId, excludeLocalOrgId);
        }
        return sysOrgMapper.selectCount(q) == 0;
    }

    private void upsertUsers(Long tenantId, Long batchId, List<ExternalUser> users, Map<String, Long> orgIdMap, SyncCounters counters) {
        if (users == null || users.isEmpty()) return;

        List<SysExternalUserMap> existingMaps = userMapMapper.selectList(new LambdaQueryWrapper<SysExternalUserMap>()
                .eq(SysExternalUserMap::getTenantId, tenantId)
                .eq(SysExternalUserMap::getPlatform, PLATFORM));
        Map<String, SysExternalUserMap> byExternalId = new HashMap<>();
        for (SysExternalUserMap m : existingMaps) {
            byExternalId.put(m.getExternalUserId(), m);
        }

        for (ExternalUser ext : users) {
            if (StringUtils.isBlank(ext.getExternalUserId())) continue;

            SysExternalUserMap map = byExternalId.get(ext.getExternalUserId());
            String desiredUsername = chooseUsername(ext);

            if (map == null) {
                // create user (handle username conflict)
                String username = resolveUsernameConflict(tenantId, desiredUsername, ext.getExternalUserId());

                SysUser u = new SysUser();
                u.setTenantId(tenantId);
                u.setUsername(username);
                u.setRealName(ext.getName());
                u.setPhone(chooseUserPhone(ext));
                u.setEmail(ext.getEmail());
                u.setIdCard(normalizeIdCard(ext.getIdCard()));
                u.setGender(mapGender(ext.getSex()));
                u.setBirthday(parseLocalDate(ext.getBirthday()));
                u.setNativePlace(StringUtils.trimToNull(ext.getNativePlace()));
                u.setEducationLevel(ext.getEducationLevel());
                u.setWorkStartDate(parseLocalDate(ext.getWorkStartDate()));
                u.setCompanyStartDate(parseLocalDate(ext.getCompanyStartDate()));
                u.setWorkYear(toBigDecimal(ext.getWorkYear()));
                u.setCompanyWorkYear(toBigDecimal(ext.getCompanyWorkYear()));
                u.setUserType(2);
                u.setUserStatus(mapExternalUserStatus(ext.getStatus()));
                u.setRemark(buildEcologyRemark(ext.getResourceId()));
                // 初始密码：默认与手机号相同（无手机时随机，避免空密码）
                u.setPassword(PasswordUtil.encrypt(initialPasswordPlain(ext)));
                sysUserMapper.insert(u);

                SysExternalUserMap newMap = new SysExternalUserMap();
                newMap.setTenantId(tenantId);
                newMap.setPlatform(PLATFORM);
                newMap.setExternalUserId(ext.getExternalUserId());
                newMap.setUserId(u.getId());
                newMap.setLoginName(username);
                newMap.setPhoneSnapshot(chooseUserPhone(ext));
                newMap.setEmailSnapshot(ext.getEmail());
                newMap.setDeptExternalId(ext.getDeptExternalId());
                newMap.setStatusSnapshot(ext.getStatus());
                newMap.setLastExternalUpdateTime(ext.getUpdateTime());
                newMap.setLastSeenBatchId(batchId);
                userMapMapper.insert(newMap);
                byExternalId.put(ext.getExternalUserId(), newMap);

                // bind main org if exists
                bindMainOrg(tenantId, u.getId(), ext.getDeptExternalId(), orgIdMap);

                counters.inserted++;
                continue;
            }

            // update existing
            boolean needUpdate = isNewer(ext.getUpdateTime(), map.getLastExternalUpdateTime())
                    || !Objects.equals(StringUtils.trimToEmpty(ext.getMobile()), StringUtils.trimToEmpty(map.getPhoneSnapshot()))
                    || !Objects.equals(StringUtils.trimToEmpty(ext.getEmail()), StringUtils.trimToEmpty(map.getEmailSnapshot()))
                    || !Objects.equals(StringUtils.trimToEmpty(ext.getDeptExternalId()), StringUtils.trimToEmpty(map.getDeptExternalId()))
                    || !Objects.equals(StringUtils.trimToEmpty(ext.getStatus()), StringUtils.trimToEmpty(map.getStatusSnapshot()));

            map.setLastSeenBatchId(batchId);
            if (!needUpdate) {
                userMapMapper.updateById(map);
                counters.skipped++;
                continue;
            }

            SysUser u = sysUserMapper.selectById(map.getUserId());
            if (u == null) {
                // 映射脏数据，按新增重建
                byExternalId.remove(ext.getExternalUserId());
                counters.skipped++;
                continue;
            }

            // username 变更：只有当 desiredUsername 非空且未冲突时才更新；否则保持原 username
            if (StringUtils.isNotBlank(desiredUsername) && !desiredUsername.equals(u.getUsername())) {
                String resolved = resolveUsernameConflict(tenantId, desiredUsername, ext.getExternalUserId(), u.getId());
                if (!resolved.equals(u.getUsername())) {
                    u.setUsername(resolved);
                }
            }

            u.setRealName(ext.getName());
            u.setPhone(chooseUserPhone(ext));
            u.setEmail(ext.getEmail());
            u.setIdCard(normalizeIdCard(ext.getIdCard()));
            u.setGender(mapGender(ext.getSex()));
            u.setBirthday(parseLocalDate(ext.getBirthday()));
            u.setNativePlace(StringUtils.trimToNull(ext.getNativePlace()));
            u.setEducationLevel(ext.getEducationLevel());
            u.setWorkStartDate(parseLocalDate(ext.getWorkStartDate()));
            u.setCompanyStartDate(parseLocalDate(ext.getCompanyStartDate()));
            u.setWorkYear(toBigDecimal(ext.getWorkYear()));
            u.setCompanyWorkYear(toBigDecimal(ext.getCompanyWorkYear()));
            u.setUserStatus(mapExternalUserStatus(ext.getStatus()));
            if (StringUtils.isNotBlank(ext.getResourceId())) {
                u.setRemark(buildEcologyRemark(ext.getResourceId()));
            }
            sysUserMapper.updateById(u);

            bindMainOrg(tenantId, u.getId(), ext.getDeptExternalId(), orgIdMap);

            map.setLoginName(u.getUsername());
            map.setPhoneSnapshot(chooseUserPhone(ext));
            map.setEmailSnapshot(ext.getEmail());
            map.setDeptExternalId(ext.getDeptExternalId());
            map.setStatusSnapshot(ext.getStatus());
            map.setLastExternalUpdateTime(ext.getUpdateTime());
            userMapMapper.updateById(map);

            counters.updated++;
        }
    }

    private void reconcileMissingUsers(Long tenantId, Long batchId, SyncCounters counters) {
        // 本批次未出现的外部用户：禁用（不删除）
        List<SysExternalUserMap> missing = userMapMapper.selectList(new LambdaQueryWrapper<SysExternalUserMap>()
                .eq(SysExternalUserMap::getTenantId, tenantId)
                .eq(SysExternalUserMap::getPlatform, PLATFORM)
                .ne(SysExternalUserMap::getLastSeenBatchId, batchId));
        for (SysExternalUserMap m : missing) {
            SysUser u = sysUserMapper.selectById(m.getUserId());
            if (u == null) continue;
            if (u.getUserStatus() != null && u.getUserStatus() == 0) continue;
            u.setUserStatus(0);
            sysUserMapper.updateById(u);
            counters.disabled++;
        }
    }

    /**
     * 手机号优先级：mobile_effective > mobile > telephone
     */
    private static String chooseUserPhone(ExternalUser ext) {
        String v = StringUtils.trimToNull(ext.getMobileEffective());
        if (v != null) return v;
        v = StringUtils.trimToNull(ext.getMobile());
        if (v != null) return v;
        return StringUtils.trimToNull(ext.getTelephone());
    }

    /**
     * 性别映射：泛微侧常见 0=男 1=女；本系统 gender: 0=未知 1=男 2=女
     */
    private static Integer mapGender(String sex) {
        String s = StringUtils.trimToEmpty(sex);
        if ("0".equals(s)) return 1;
        if ("1".equals(s)) return 2;
        return 0;
    }

    /**
     * 证件号清洗与长度保护：去空白、转大写，超长时截断，避免写库失败。
     * 说明：本系统字段名叫 id_card，但外部可能传护照/其他证件号，长度不一定为18。
     */
    private static String normalizeIdCard(String raw) {
        String v = StringUtils.trimToNull(raw);
        if (v == null) return null;
        v = v.replaceAll("\\s+", "").toUpperCase();
        // MySQL 列已扩到 varchar(32)，这里再兜底一次
        if (v.length() > 32) {
            v = v.substring(0, 32);
        }
        return v;
    }

    private static LocalDate parseLocalDate(String yyyyMMdd) {
        String s = StringUtils.trimToEmpty(yyyyMMdd);
        if (StringUtils.isBlank(s)) return null;
        try {
            // 期望格式：yyyy-MM-dd
            return LocalDate.parse(s);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static BigDecimal toBigDecimal(Double v) {
        if (v == null) return null;
        // 避免二进制浮点误差扩大：保留两位小数
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private void bindMainOrg(Long tenantId, Long userId, String deptExternalId, Map<String, Long> orgIdMap) {
        if (StringUtils.isBlank(deptExternalId)) return;
        Long orgId = orgIdMap.get(deptExternalId);
        if (orgId == null) return;

        // 将该组织设为主组织（存在则更新 is_main；不存在则插入）
        List<SysUserOrg> existing = sysUserOrgMapper.selectList(new LambdaQueryWrapper<SysUserOrg>()
                .eq(SysUserOrg::getUserId, userId));
        SysUserOrg currentMain = existing.stream().filter(x -> x.getIsMain() != null && x.getIsMain() == 1).findFirst().orElse(null);
        SysUserOrg target = existing.stream().filter(x -> Objects.equals(x.getOrgId(), orgId)).findFirst().orElse(null);

        if (target == null) {
            SysUserOrg nu = new SysUserOrg();
            nu.setTenantId(tenantId);
            nu.setUserId(userId);
            nu.setOrgId(orgId);
            nu.setIsMain(1);
            sysUserOrgMapper.insert(nu);
        } else if (target.getIsMain() == null || target.getIsMain() != 1) {
            target.setIsMain(1);
            sysUserOrgMapper.updateById(target);
        }

        if (currentMain != null && !Objects.equals(currentMain.getOrgId(), orgId)) {
            currentMain.setIsMain(0);
            sysUserOrgMapper.updateById(currentMain);
        }
    }

    private static boolean isNewer(Long extUpdate, Long lastUpdate) {
        if (extUpdate == null) return false;
        if (lastUpdate == null) return true;
        return extUpdate > lastUpdate;
    }

    private int mapExternalUserStatus(String status) {
        String s = StringUtils.trimToEmpty(status);
        if (properties.getUserStatusMap() != null && properties.getUserStatusMap().containsKey(s)) {
            Integer v = properties.getUserStatusMap().get(s);
            return v != null ? v : 0;
        }
        if ("1".equals(s)) {
            return 1;
        }
        try {
            if (s.matches("-?\\d+")) {
                int n = Integer.parseInt(s);
                if (n == 1) {
                    return 1;
                }
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return 0;
    }

    private static String buildEcologyRemark(String resourceId) {
        if (StringUtils.isBlank(resourceId)) {
            return null;
        }
        String r = "ecology:rid=" + resourceId.trim();
        return r.length() > 500 ? r.substring(0, 500) : r;
    }

    private static int parseOrgStatus(String status) {
        return "1".equals(StringUtils.trimToEmpty(status)) ? 1 : 0;
    }

    /**
     * 同步新建用户时的初始密码明文：与手机号一致；无手机时随机生成，避免违反非空约束。
     */
    private static String initialPasswordPlain(ExternalUser ext) {
        String phone = StringUtils.trimToEmpty(chooseUserPhone(ext));
        if (StringUtils.isNotBlank(phone)) {
            return phone;
        }
        return RandomUtil.randomString(16);
    }

    private String chooseUsername(ExternalUser ext) {
        String phone = StringUtils.trimToEmpty(chooseUserPhone(ext));
        if (StringUtils.isNotBlank(phone)) return phone;
        String email = StringUtils.trimToEmpty(ext.getEmail());
        if (StringUtils.isNotBlank(email)) return email;
        return PLATFORM + "_" + ext.getExternalUserId();
    }

    private String resolveUsernameConflict(Long tenantId, String desiredUsername, String externalUserId) {
        return resolveUsernameConflict(tenantId, desiredUsername, externalUserId, null);
    }

    private String resolveUsernameConflict(Long tenantId, String desiredUsername, String externalUserId, Long selfUserId) {
        if (StringUtils.isBlank(desiredUsername)) {
            return PLATFORM + "_" + externalUserId;
        }
        // sys_user_unique: (tenant_id, username)
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getUsername, desiredUsername);
        SysUser exists = sysUserMapper.selectOne(q);
        if (exists == null) return desiredUsername;
        if (selfUserId != null && Objects.equals(exists.getId(), selfUserId)) return desiredUsername;
        return PLATFORM + "_" + externalUserId;
    }

    /**
     * 根据父组织ID构建 ancestors（示例实现：0 或 0,1,2）。\n
     * 这里不去递归查询全链，避免性能问题；只保证非空且可用。\n
     * 若需要严格 ancestors，可后续补“组织树重建”批处理。
     */
    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0L) return "0";
        SysOrg parent = sysOrgMapper.selectById(parentId);
        if (parent == null || StringUtils.isBlank(parent.getAncestors())) return "0," + parentId;
        return parent.getAncestors() + "," + parentId;
    }

    private static class SyncCounters {
        int inserted = 0;
        int updated = 0;
        int disabled = 0;
        int skipped = 0;
    }
}

