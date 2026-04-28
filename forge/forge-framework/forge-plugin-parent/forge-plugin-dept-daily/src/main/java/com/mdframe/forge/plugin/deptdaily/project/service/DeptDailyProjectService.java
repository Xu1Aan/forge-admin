package com.mdframe.forge.plugin.deptdaily.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectCreateRequest;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectFinishRequest;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectPageQuery;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectUpdateRequest;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProject;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProjectMember;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProjectStatusLog;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyProjectMapper;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyProjectMemberMapper;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyProjectStatusLogMapper;
import com.mdframe.forge.plugin.deptdaily.project.vo.ProjectListRowVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DeptDailyProjectService {

    private final DeptDailyProjectMapper projectMapper;
    private final DeptDailyProjectMemberMapper memberMapper;
    private final DeptDailyProjectStatusLogMapper logMapper;

    public IPage<ProjectListRowVO> page(ProjectPageQuery query) {
        Long tenantId = tenantOrDefault();
        return projectMapper.selectProjectPage(
                query.toPage(),
                tenantId,
                query.getDeptId(),
                query.getOfficeId(),
                query.getYear(),
                StringUtils.trimToNull(query.getKeyword()),
                StringUtils.trimToNull(query.getStatus())
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

    @Transactional(rollbackFor = Exception.class)
    public Long create(ProjectCreateRequest req) {
        if (req == null) throw new IllegalArgumentException("参数不能为空");
        if (StringUtils.isBlank(req.getProjectName())) throw new IllegalArgumentException("项目名不能为空");
        if (req.getLeaderUserId() == null) throw new IllegalArgumentException("项目负责人不能为空");
        if (req.getPlanEndDate() == null) throw new IllegalArgumentException("预计截止时间不能为空");

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

