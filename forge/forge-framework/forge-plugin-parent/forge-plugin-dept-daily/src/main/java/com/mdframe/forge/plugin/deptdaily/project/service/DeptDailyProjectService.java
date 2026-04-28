package com.mdframe.forge.plugin.deptdaily.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.deptdaily.project.enums.ProjectCategory;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectCreateRequest;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectFinishRequest;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectPageQuery;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectUpdateRequest;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectExcelImportResult;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectExcelImportRow;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProject;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProjectMonthReport;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProjectMember;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProjectStatusLog;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyUserMonthReportItem;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyProjectMapper;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyProjectMemberMapper;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyProjectMonthReportMapper;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyProjectStatusLogMapper;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyUserMonthReportItemMapper;
import com.mdframe.forge.plugin.deptdaily.project.vo.ProjectListRowVO;
import com.mdframe.forge.plugin.deptdaily.project.vo.ProjectMemberRowVO;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailySysUserLite;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailySysUserOrgLite;
import com.mdframe.forge.plugin.deptdaily.overview.mapper.DeptDailySysUserOrgLiteMapper;
import com.mdframe.forge.plugin.deptdaily.overview.mapper.DeptDailySysUserLiteMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptDailyProjectService {

    private static final Pattern MEMBER_SPLIT = Pattern.compile("[,，、;；\\s]+");

    private final DeptDailyProjectMapper projectMapper;
    private final DeptDailyProjectMemberMapper memberMapper;
    private final DeptDailyProjectStatusLogMapper logMapper;
    private final DeptDailyUserMonthReportItemMapper userItemMapper;
    private final DeptDailyProjectMonthReportMapper projectReportMapper;
    private final DeptDailySysUserLiteMapper userLiteMapper;
    private final DeptDailySysUserOrgLiteMapper userOrgLiteMapper;

    public IPage<ProjectListRowVO> page(ProjectPageQuery query) {
        Long tenantId = tenantOrDefault();
        return projectMapper.selectProjectPage(
                query.toPage(),
                tenantId,
                query.getDeptId(),
                query.getOfficeId(),
                query.getYear(),
                StringUtils.trimToNull(query.getKeyword()),
                StringUtils.trimToNull(query.getStatus()),
                StringUtils.trimToNull(query.getProjectCategory())
        );
    }

    public DeptDailyProject getById(Long id) {
        if (id == null) return null;
        return projectMapper.selectById(id);
    }

    public List<DeptDailyProjectMember> listMembers(Long projectId) {
        Long tenantId = tenantOrDefault();
        return memberMapper.selectList(new LambdaQueryWrapper<DeptDailyProjectMember>()
                .eq(DeptDailyProjectMember::getTenantId, tenantId)
                .eq(DeptDailyProjectMember::getProjectId, projectId)
                .eq(DeptDailyProjectMember::getIsActive, 1));
    }

    /**
     * 项目成员展示行（含用户名、组织、电话、项目内角色）。
     */
    public List<ProjectMemberRowVO> listMemberRows(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        return projectMapper.selectMemberRows(tenantOrDefault(), projectId);
    }

    /**
     * 按 userId 列表返回用户简报（顺序与入参首次出现顺序一致，去重）。用于前端选人后刷新表格。
     */
    public List<ProjectMemberRowVO> listMemberBriefsOrdered(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Long tenantId = tenantOrDefault();
        List<Long> orderedUnique = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long id : userIds) {
            if (id == null || seen.contains(id)) {
                continue;
            }
            seen.add(id);
            orderedUnique.add(id);
        }
        if (orderedUnique.isEmpty()) {
            return List.of();
        }
        List<ProjectMemberRowVO> found = projectMapper.selectMemberBriefsByIds(tenantId, orderedUnique);
        Map<Long, ProjectMemberRowVO> byId = found.stream()
                .filter(r -> r.getUserId() != null)
                .collect(Collectors.toMap(ProjectMemberRowVO::getUserId, Function.identity(), (a, b) -> a));
        List<ProjectMemberRowVO> out = new ArrayList<>(orderedUnique.size());
        for (Long uid : orderedUnique) {
            ProjectMemberRowVO row = byId.get(uid);
            if (row != null) {
                out.add(row);
            }
        }
        return out;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectCreateRequest req) {
        if (req == null) throw new IllegalArgumentException("参数不能为空");
        if (StringUtils.isBlank(req.getProjectName())) throw new IllegalArgumentException("项目名不能为空");
        if (req.getLeaderUserId() == null) throw new IllegalArgumentException("项目负责人不能为空");
        if (req.getPlanEndDate() == null) throw new IllegalArgumentException("预计截止时间不能为空");
        String cat = StringUtils.trimToNull(req.getProjectCategory());
        if (cat == null) throw new IllegalArgumentException("请选择项目类别");
        if (!ProjectCategory.isValid(cat)) throw new IllegalArgumentException("项目类别无效");

        Long tenantId = tenantOrDefault();
        Long uid = requireUserId();

        LocalDate start = req.getStartDate() != null ? req.getStartDate() : LocalDate.now();
        if (req.getPlanEndDate().isBefore(start)) {
            throw new IllegalArgumentException("预计截止时间不能早于立项时间");
        }

        DeptDailyProject p = new DeptDailyProject();
        p.setTenantId(tenantId);
        p.setDeptId(req.getDeptId());
        p.setOfficeId(req.getOfficeId());
        p.setProjectName(StringUtils.trim(req.getProjectName()));
        p.setProjectCategory(cat);
        p.setLeaderUserId(req.getLeaderUserId());
        p.setStartDate(start);
        p.setPlanEndDate(req.getPlanEndDate());
        p.setStatus("ACTIVE");
        p.setRemark(StringUtils.trimToNull(req.getRemark()));
        projectMapper.insert(p);

        upsertMembersInternal(tenantId, uid, p.getId(), req.getLeaderUserId(), req.getMemberUserIds());
        return p.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(ProjectUpdateRequest req) {
        if (req == null || req.getId() == null) throw new IllegalArgumentException("id不能为空");
        Long tenantId = tenantOrDefault();
        Long uid = requireUserId();

        DeptDailyProject existing = projectMapper.selectOne(new LambdaQueryWrapper<DeptDailyProject>()
                .eq(DeptDailyProject::getTenantId, tenantId)
                .eq(DeptDailyProject::getId, req.getId())
                .last("limit 1"));
        if (existing == null) throw new IllegalArgumentException("项目不存在");

        if (StringUtils.isNotBlank(req.getProjectName())) existing.setProjectName(StringUtils.trim(req.getProjectName()));
        if (req.getProjectCategory() != null) {
            String c = StringUtils.trimToNull(req.getProjectCategory());
            if (c == null) {
                throw new IllegalArgumentException("项目类别不能为空");
            }
            if (!ProjectCategory.isValid(c)) {
                throw new IllegalArgumentException("项目类别无效");
            }
            existing.setProjectCategory(c);
        }
        if (req.getDeptId() != null) existing.setDeptId(req.getDeptId());
        if (req.getOfficeId() != null) existing.setOfficeId(req.getOfficeId());
        if (req.getLeaderUserId() != null) existing.setLeaderUserId(req.getLeaderUserId());
        if (req.getStartDate() != null) existing.setStartDate(req.getStartDate());
        if (req.getPlanEndDate() != null) existing.setPlanEndDate(req.getPlanEndDate());
        if (req.getRemark() != null) existing.setRemark(StringUtils.trimToNull(req.getRemark()));

        if (existing.getPlanEndDate() != null && existing.getStartDate() != null
                && existing.getPlanEndDate().isBefore(existing.getStartDate())) {
            throw new IllegalArgumentException("预计截止时间不能早于立项时间");
        }

        projectMapper.updateById(existing);
        upsertMembersInternal(tenantId, uid, existing.getId(), existing.getLeaderUserId(), req.getMemberUserIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void finish(Long projectId, ProjectFinishRequest req) {
        if (projectId == null) throw new IllegalArgumentException("projectId不能为空");
        if (req == null || req.getDone() == null) throw new IllegalArgumentException("done不能为空");
        Long tenantId = tenantOrDefault();
        Long uid = requireUserId();

        DeptDailyProject p = projectMapper.selectOne(new LambdaQueryWrapper<DeptDailyProject>()
                .eq(DeptDailyProject::getTenantId, tenantId)
                .eq(DeptDailyProject::getId, projectId)
                .last("limit 1"));
        if (p == null) throw new IllegalArgumentException("项目不存在");

        // 权限：负责人 / 超管 / 租户管理员 / 有权限标识
        boolean can = uid.equals(p.getLeaderUserId())
                || SessionHelper.isAdmin()
                || SessionHelper.isTenantAdmin()
                || SessionHelper.hasPermission("dept-daily:project:finish");
        if (!can) throw new IllegalStateException("无权限操作");

        String from = p.getStatus();
        String to = req.getDone() ? "DONE" : "ACTIVE";
        if (StringUtils.equals(from, to)) return;

        p.setStatus(to);
        if ("DONE".equals(to)) {
            p.setDoneAt(LocalDateTime.now());
            p.setDoneByUserId(uid);
        } else {
            p.setDoneAt(null);
            p.setDoneByUserId(null);
        }
        projectMapper.updateById(p);

        DeptDailyProjectStatusLog log = new DeptDailyProjectStatusLog();
        log.setTenantId(tenantId);
        log.setProjectId(projectId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setReason(StringUtils.trimToNull(req.getReason()));
        log.setOperatedAt(LocalDateTime.now());
        log.setOperatedBy(uid);
        logMapper.insert(log);
    }

    /**
     * 删除项目（谨慎）：仅允许删除“未产生任何月报数据”的项目。
     * <p>
     * 删除会同时清理成员关系、状态日志；如已有个人月报/项目月报数据将直接拒绝（建议改用“完成/关闭”）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long projectId, String reason) {
        if (projectId == null) throw new IllegalArgumentException("projectId不能为空");
        Long tenantId = tenantOrDefault();
        Long uid = requireUserId();

        DeptDailyProject p = projectMapper.selectOne(new LambdaQueryWrapper<DeptDailyProject>()
                .eq(DeptDailyProject::getTenantId, tenantId)
                .eq(DeptDailyProject::getId, projectId)
                .last("limit 1"));
        if (p == null) throw new IllegalArgumentException("项目不存在");

        // 权限：负责人 / 超管 / 租户管理员 / 有权限标识
        boolean can = uid.equals(p.getLeaderUserId())
                || SessionHelper.isAdmin()
                || SessionHelper.isTenantAdmin()
                || SessionHelper.hasPermission("dept-daily:project:delete");
        if (!can) throw new IllegalStateException("无权限操作");

        // 若已有任何月报数据，禁止删除
        Long userItemCnt = userItemMapper.selectCount(new LambdaQueryWrapper<DeptDailyUserMonthReportItem>()
                .eq(DeptDailyUserMonthReportItem::getTenantId, tenantId)
                .eq(DeptDailyUserMonthReportItem::getProjectId, projectId));
        if (userItemCnt != null && userItemCnt > 0) {
            throw new IllegalStateException("该项目已产生个人月报数据，禁止删除。请改用“完成项目/关闭项目”处理。");
        }
        Long projReportCnt = projectReportMapper.selectCount(new LambdaQueryWrapper<DeptDailyProjectMonthReport>()
                .eq(DeptDailyProjectMonthReport::getTenantId, tenantId)
                .eq(DeptDailyProjectMonthReport::getProjectId, projectId));
        if (projReportCnt != null && projReportCnt > 0) {
            throw new IllegalStateException("该项目已产生项目月报数据，禁止删除。请改用“完成项目/关闭项目”处理。");
        }

        // 记录一次状态日志（便于审计：删除原因）
        DeptDailyProjectStatusLog log = new DeptDailyProjectStatusLog();
        log.setTenantId(tenantId);
        log.setProjectId(projectId);
        log.setFromStatus(p.getStatus());
        log.setToStatus("DELETED");
        log.setReason(StringUtils.trimToNull(reason));
        log.setOperatedAt(LocalDateTime.now());
        log.setOperatedBy(uid);
        logMapper.insert(log);

        // 清理成员关系（全量）
        memberMapper.delete(new LambdaQueryWrapper<DeptDailyProjectMember>()
                .eq(DeptDailyProjectMember::getTenantId, tenantId)
                .eq(DeptDailyProjectMember::getProjectId, projectId));

        // 删除项目本身
        projectMapper.delete(new LambdaQueryWrapper<DeptDailyProject>()
                .eq(DeptDailyProject::getTenantId, tenantId)
                .eq(DeptDailyProject::getId, projectId));
    }

    /**
     * 供个人月报填报：仅返回未截止且本人参与/负责的项目
     */
    public List<DeptDailyProject> listFillableProjects(Long deptId, Long officeId) {
        Long tenantId = tenantOrDefault();
        Long uid = requireUserId();
        LocalDate today = LocalDate.now();

        // 参与项目（成员表）
        List<Long> memberProjectIds = memberMapper.selectList(new LambdaQueryWrapper<DeptDailyProjectMember>()
                        .eq(DeptDailyProjectMember::getTenantId, tenantId)
                        .eq(DeptDailyProjectMember::getUserId, uid)
                        .eq(DeptDailyProjectMember::getIsActive, 1))
                .stream()
                .map(DeptDailyProjectMember::getProjectId)
                .distinct()
                .toList();

        LambdaQueryWrapper<DeptDailyProject> w = new LambdaQueryWrapper<DeptDailyProject>()
                .eq(DeptDailyProject::getTenantId, tenantId)
                .notIn(DeptDailyProject::getStatus, List.of("DONE", "CLOSED"))
                .ge(DeptDailyProject::getPlanEndDate, today)
                .and(q -> q.eq(DeptDailyProject::getLeaderUserId, uid)
                        .or()
                        .in(!memberProjectIds.isEmpty(), DeptDailyProject::getId, memberProjectIds));

        if (deptId != null) w.eq(DeptDailyProject::getDeptId, deptId);
        if (officeId != null) w.eq(DeptDailyProject::getOfficeId, officeId);

        return projectMapper.selectList(w.orderByAsc(DeptDailyProject::getPlanEndDate).orderByDesc(DeptDailyProject::getId));
    }

    /**
     * Excel 导入：按“姓名/用户名”匹配 sys_user，创建项目与成员关系。
     * <p>
     * - dryRun=true：仅校验与解析，不落库
     * - dryRun=false：落库（事务内逐行处理；失败行跳过并记录原因）
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectExcelImportResult importProjectsFromExcelRows(List<ProjectExcelImportRow> rows,
                                                                boolean dryRun,
                                                                LocalDate defaultStartDate,
                                                                LocalDate defaultPlanEndDate) {
        ProjectExcelImportResult result = new ProjectExcelImportResult();
        if (rows == null || rows.isEmpty()) {
            result.setTotal(0);
            result.setSuccess(0);
            result.setFailed(0);
            return result;
        }

        if (defaultStartDate == null) defaultStartDate = LocalDate.now();
        if (defaultPlanEndDate == null) defaultPlanEndDate = defaultStartDate.plusMonths(6);
        if (defaultPlanEndDate.isBefore(defaultStartDate)) {
            throw new IllegalArgumentException("默认预计截止日期不能早于默认立项日期");
        }

        Long tenantId = tenantOrDefault();
        Long uid = requireUserId();

        int total = 0, ok = 0, bad = 0;

        // EasyExcel: 读取时默认不含表头行；我们这里按“数据行起始=2”来回显行号
        for (int i = 0; i < rows.size(); i++) {
            int excelRowNum = i + 2;
            ProjectExcelImportRow r = rows.get(i);
            if (r == null) continue;
            total++;

            String projectName = StringUtils.trimToNull(r.getProjectName());
            String projectType = StringUtils.trimToNull(r.getProjectType());
            String leaderName = StringUtils.trimToNull(r.getLeaderName());
            String memberNames = StringUtils.trimToNull(r.getMemberNames());

            try {
                if (projectName == null) throw new IllegalArgumentException("项目名为空");
                if (leaderName == null) throw new IllegalArgumentException("项目负责人为空");

                String category = resolveProjectCategory(projectType);
                Long leaderUserId = resolveUserIdByName(tenantId, leaderName);

                // 成员：允许为空（仅负责人）；否则按分隔符切分
                Set<Long> memberUserIds = new HashSet<>();
                if (memberNames != null) {
                    for (String name : MEMBER_SPLIT.split(memberNames)) {
                        String n = StringUtils.trimToNull(name);
                        if (n == null) continue;
                        memberUserIds.add(resolveUserIdByName(tenantId, n));
                    }
                }
                memberUserIds.add(leaderUserId);

                if (!dryRun) {
                    // 幂等规则：同名但不同类别视为不同项目，因此按（项目名+项目类别）匹配“已存在”
                    DeptDailyProject existing = projectMapper.selectOne(new LambdaQueryWrapper<DeptDailyProject>()
                            .eq(DeptDailyProject::getTenantId, tenantId)
                            .eq(DeptDailyProject::getProjectName, projectName)
                            .eq(DeptDailyProject::getProjectCategory, category)
                            .last("limit 1"));

                    if (existing != null) {
                        // 已存在：按导入内容刷新负责人/类别，并更新成员关系（避免重复导入报错）
                        existing.setProjectCategory(category);
                        existing.setLeaderUserId(leaderUserId);
                        if (StringUtils.isBlank(existing.getStatus())) {
                            existing.setStatus("ACTIVE");
                        }
                        projectMapper.updateById(existing);
                        upsertMembersInternal(tenantId, uid, existing.getId(), leaderUserId, new ArrayList<>(memberUserIds));
                        result.getUpdatedProjectIds().add(existing.getId());
                    } else {
                        DeptDailyProject p = new DeptDailyProject();
                        p.setTenantId(tenantId);
                        p.setDeptId(null);
                        p.setOfficeId(null);
                        p.setProjectName(projectName);
                        p.setProjectCategory(category);
                        p.setLeaderUserId(leaderUserId);
                        p.setStartDate(defaultStartDate);
                        p.setPlanEndDate(defaultPlanEndDate);
                        p.setStatus("ACTIVE");
                        projectMapper.insert(p);

                        upsertMembersInternal(tenantId, uid, p.getId(), leaderUserId, new ArrayList<>(memberUserIds));
                        result.getCreatedProjectIds().add(p.getId());
                    }
                }
                ok++;
            } catch (Exception ex) {
                bad++;
                String msg = ex.getMessage() != null ? ex.getMessage() : "导入失败";
                // 兜底：把 DB 唯一键异常转成更友好的提示
                if (msg.contains("uk_dd_proj_tenant_name") || msg.contains("Duplicate entry")) {
                    msg = "项目已存在（同一租户下项目名重复），已跳过或请使用“更新模式”";
                }
                result.addError(excelRowNum, projectName, msg);
            }
        }

        result.setTotal(total);
        result.setSuccess(ok);
        result.setFailed(bad);
        return result;
    }

    private String resolveProjectCategory(String projectType) {
        // 允许直接传 code；或传中文 label；空/未知按 OTHER
        if (StringUtils.isBlank(projectType)) return ProjectCategory.OTHER;
        String t = projectType.trim();
        if (ProjectCategory.isValid(t)) return t;
        for (var e : ProjectCategory.allLabels().entrySet()) {
            if (t.equals(e.getValue())) return e.getKey();
        }
        return ProjectCategory.OTHER;
    }

    private Long resolveUserIdByName(Long tenantId, String nameOrUsername) {
        String n = StringUtils.trimToNull(nameOrUsername);
        if (n == null) throw new IllegalArgumentException("人员姓名为空");

        List<DeptDailySysUserLite> list = userLiteMapper.selectList(new LambdaQueryWrapper<DeptDailySysUserLite>()
                .eq(DeptDailySysUserLite::getTenantId, tenantId)
                .eq(DeptDailySysUserLite::getUserStatus, 1)
                .and(w -> w.eq(DeptDailySysUserLite::getRealName, n).or().eq(DeptDailySysUserLite::getUsername, n)));

        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("未找到人员：" + n);
        }
        if (list.size() > 1) {
            // 同名优先匹配“本科室/主组织”（当前操作人主组织），若本科室不存在则按严格策略报错
            Long mainOrgId = SessionHelper.getMainOrgId();
            if (mainOrgId != null) {
                List<Long> userIds = list.stream().map(DeptDailySysUserLite::getId).toList();
                List<DeptDailySysUserOrgLite> orgRows = userOrgLiteMapper.selectList(new LambdaQueryWrapper<DeptDailySysUserOrgLite>()
                        .eq(DeptDailySysUserOrgLite::getTenantId, tenantId)
                        .in(DeptDailySysUserOrgLite::getUserId, userIds)
                        .eq(DeptDailySysUserOrgLite::getIsMain, 1)
                        .eq(DeptDailySysUserOrgLite::getOrgId, mainOrgId));
                if (orgRows != null && orgRows.size() == 1) {
                    return orgRows.get(0).getUserId();
                }
            }
            throw new IllegalArgumentException("人员姓名重复，无法唯一匹配：" + n);
        }
        return list.get(0).getId();
    }

    private void upsertMembersInternal(Long tenantId, Long uid, Long projectId, Long leaderUserId, List<Long> memberUserIds) {
        Set<Long> members = new HashSet<>();
        if (memberUserIds != null) {
            for (Long mid : memberUserIds) {
                if (mid != null) members.add(mid);
            }
        }
        if (leaderUserId != null) members.add(leaderUserId);

        // 先全部置为 inactive
        memberMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DeptDailyProjectMember>()
                .eq(DeptDailyProjectMember::getTenantId, tenantId)
                .eq(DeptDailyProjectMember::getProjectId, projectId)
                .set(DeptDailyProjectMember::getIsActive, 0)
                .set(DeptDailyProjectMember::getLeftAt, LocalDateTime.now()));

        for (Long mid : members) {
            DeptDailyProjectMember existing = memberMapper.selectOne(new LambdaQueryWrapper<DeptDailyProjectMember>()
                    .eq(DeptDailyProjectMember::getTenantId, tenantId)
                    .eq(DeptDailyProjectMember::getProjectId, projectId)
                    .eq(DeptDailyProjectMember::getUserId, mid)
                    .last("limit 1"));
            if (existing == null) {
                DeptDailyProjectMember m = new DeptDailyProjectMember();
                m.setTenantId(tenantId);
                m.setProjectId(projectId);
                m.setUserId(mid);
                m.setRole(mid.equals(leaderUserId) ? "LEADER" : "MEMBER");
                m.setJoinedAt(LocalDateTime.now());
                m.setIsActive(1);
                memberMapper.insert(m);
            } else {
                existing.setIsActive(1);
                existing.setLeftAt(null);
                existing.setRole(mid.equals(leaderUserId) ? "LEADER" : "MEMBER");
                memberMapper.updateById(existing);
            }
        }
    }

    private static Long requireUserId() {
        Long uid = SessionHelper.getUserId();
        if (uid == null) throw new IllegalStateException("未登录");
        return uid;
    }

    private static Long tenantOrDefault() {
        Long tid = SessionHelper.getTenantId();
        return tid != null ? tid : 1L;
    }
}

