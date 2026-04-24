package com.mdframe.forge.plugin.system.external.weaver;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.external.weaver.model.ExternalOrg;
import com.mdframe.forge.plugin.system.external.weaver.model.ExternalUser;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 手动/定时触发同步（单租户版本）
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

        WeaverSyncResult result = new WeaverSyncResult();
        result.setBatchId(batch.getId());

        try {
            WeaverClient.Snapshot snapshot = weaverClient.fetchSnapshot();
            batch.setRawSnapshotHash(snapshot.getRawHash());

            List<ExternalOrg> orgs = snapshot.getOrgs() != null ? snapshot.getOrgs() : List.of();
            List<ExternalUser> users = snapshot.getUsers() != null ? snapshot.getUsers() : List.of();
            batch.setFetchedOrgCount(orgs.size());
            batch.setFetchedUserCount(users.size());

            SyncCounters counters = new SyncCounters();

            Map<String, Long> externalOrgIdToLocalOrgId = upsertOrgs(tenantId, batch.getId(), orgs, counters);
            upsertUsers(tenantId, batch.getId(), users, externalOrgIdToLocalOrgId, counters);

            reconcileMissingUsers(tenantId, batch.getId(), counters);
            // 组织的 missing 先不做禁用：组织禁用可能影响大量用户；可后续加配置项

            batch.setInsertedCount(counters.inserted);
            batch.setUpdatedCount(counters.updated);
            batch.setDisabledCount(counters.disabled);
            batch.setSkippedCount(counters.skipped);
            batch.setStatus("success");
            batch.setEndedAt(LocalDateTime.now());
            batchMapper.updateById(batch);

            result.setStatus(batch.getStatus());
            result.setFetchedOrgCount(batch.getFetchedOrgCount());
            result.setFetchedUserCount(batch.getFetchedUserCount());
            result.setInsertedCount(counters.inserted);
            result.setUpdatedCount(counters.updated);
            result.setDisabledCount(counters.disabled);
            result.setSkippedCount(counters.skipped);
            return result;
        } catch (Exception e) {
            log.error("Weaver 同步失败 batchId={}", batch.getId(), e);
            batch.setStatus("failed");
            batch.setErrorMessage(e.getMessage());
            batch.setEndedAt(LocalDateTime.now());
            batchMapper.updateById(batch);
            result.setStatus("failed");
            throw e;
        }
    }

    private Map<String, Long> upsertOrgs(Long tenantId, Long batchId, List<ExternalOrg> orgs, SyncCounters counters) {
        if (orgs == null || orgs.isEmpty()) return new HashMap<>();

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
                // create org
                SysOrg org = new SysOrg();
                org.setTenantId(tenantId);
                org.setOrgName(StringUtils.defaultIfBlank(ext.getName(), ext.getExternalOrgId()));
                org.setOrgType(2);
                org.setOrgStatus(parseOrgStatus(ext.getStatus()));
                org.setSort(0);
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
                    org.setOrgName(StringUtils.defaultIfBlank(ext.getName(), org.getOrgName()));
                    org.setOrgStatus(parseOrgStatus(ext.getStatus()));
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

            Long parentLocalId = 0L;
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
                u.setPhone(ext.getMobile());
                u.setEmail(ext.getEmail());
                u.setUserType(2);
                u.setUserStatus(parseUserStatus(ext.getStatus()));
                // 初始密码：随机（避免与泛微密码耦合）；可后续接SSO
                u.setPassword(PasswordUtil.encrypt(StrUtil.randomString(16)));
                sysUserMapper.insert(u);

                SysExternalUserMap newMap = new SysExternalUserMap();
                newMap.setTenantId(tenantId);
                newMap.setPlatform(PLATFORM);
                newMap.setExternalUserId(ext.getExternalUserId());
                newMap.setUserId(u.getId());
                newMap.setLoginName(username);
                newMap.setPhoneSnapshot(ext.getMobile());
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
            u.setPhone(ext.getMobile());
            u.setEmail(ext.getEmail());
            u.setUserStatus(parseUserStatus(ext.getStatus()));
            sysUserMapper.updateById(u);

            bindMainOrg(tenantId, u.getId(), ext.getDeptExternalId(), orgIdMap);

            map.setLoginName(u.getUsername());
            map.setPhoneSnapshot(ext.getMobile());
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

    private static int parseUserStatus(String status) {
        return "1".equals(StringUtils.trimToEmpty(status)) ? 1 : 0;
    }

    private static int parseOrgStatus(String status) {
        return "1".equals(StringUtils.trimToEmpty(status)) ? 1 : 0;
    }

    private String chooseUsername(ExternalUser ext) {
        String mobile = StringUtils.trimToEmpty(ext.getMobile());
        if (StringUtils.isNotBlank(mobile)) return mobile;
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

