package com.mdframe.forge.plugin.deptdaily.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectMonthReportUpsertRequest;
import com.mdframe.forge.plugin.deptdaily.project.dto.UserMonthReportUpsertRequest;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProject;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProjectMonthReport;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyUserMonthReportItem;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyProjectMapper;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyProjectMonthReportMapper;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyUserMonthReportItemMapper;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailyUserMonthReportSheet;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailySysUserLite;
import com.mdframe.forge.plugin.deptdaily.overview.mapper.DeptDailyUserMonthReportSheetMapper;
import com.mdframe.forge.plugin.deptdaily.overview.mapper.DeptDailySysUserLiteMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DeptDailyReportService {

    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final DeptDailyProjectMapper projectMapper;
    private final DeptDailyUserMonthReportItemMapper userItemMapper;
    private final DeptDailyProjectMonthReportMapper projectReportMapper;
    private final DeptDailyUserMonthReportSheetMapper userSheetMapper;
    private final DeptDailySysUserLiteMapper userLiteMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long upsertUserMonthItem(UserMonthReportUpsertRequest req) {
        if (req == null) throw new IllegalArgumentException("参数不能为空");
        if (StringUtils.isBlank(req.getReportYm())) throw new IllegalArgumentException("reportYm不能为空");
        if (req.getProjectId() == null) throw new IllegalArgumentException("projectId不能为空");
        if (StringUtils.isBlank(req.getProgressText())) throw new IllegalArgumentException("进展情况不能为空");

        YearMonth ym = parseYm(req.getReportYm());
        Long tenantId = tenantOrDefault();
        Long uid = requireUserId();

        DeptDailyProject p = projectMapper.selectOne(new LambdaQueryWrapper<DeptDailyProject>()
                .eq(DeptDailyProject::getTenantId, tenantId)
                .eq(DeptDailyProject::getId, req.getProjectId())
                .last("limit 1"));
        if (p == null) throw new IllegalArgumentException("项目不存在");

        // 未截止：按当前日期判断（若需要按月判断，可替换为 ym.atEndOfMonth）
        if (p.getPlanEndDate() != null && p.getPlanEndDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("项目已截止，无法填报");
        }

        DeptDailyUserMonthReportItem existing = userItemMapper.selectOne(new LambdaQueryWrapper<DeptDailyUserMonthReportItem>()
                .eq(DeptDailyUserMonthReportItem::getTenantId, tenantId)
                .eq(DeptDailyUserMonthReportItem::getReportYm, ym.format(YM_FMT))
                .eq(DeptDailyUserMonthReportItem::getProjectId, req.getProjectId())
                .eq(DeptDailyUserMonthReportItem::getUserId, uid)
                .last("limit 1"));

        boolean submit = Boolean.TRUE.equals(req.getSubmit());

        // 先确保月报总表存在并允许修改
        upsertUserSheet(tenantId, uid, p.getDeptId(), p.getOfficeId(), ym.format(YM_FMT), submit);

        if (existing == null) {
            String status = submit ? "SUBMITTED" : "DRAFT";
            LocalDateTime submittedAt = submit ? LocalDateTime.now() : null;
            DeptDailyUserMonthReportItem it = new DeptDailyUserMonthReportItem();
            it.setTenantId(tenantId);
            it.setDeptId(p.getDeptId());
            it.setOfficeId(p.getOfficeId());
            it.setReportYm(ym.format(YM_FMT));
            it.setProjectId(req.getProjectId());
            it.setUserId(uid);
            it.setProgressText(StringUtils.trim(req.getProgressText()));
            it.setWorkDays(req.getWorkDays());
            it.setBlockers(StringUtils.trimToNull(req.getBlockers()));
            it.setNextPlan(StringUtils.trimToNull(req.getNextPlan()));
            it.setStatus(status);
            it.setSubmittedAt(submittedAt);
            userItemMapper.insert(it);
            return it.getId();
        }

        // 已提交后仍可自动保存草稿内容；草稿保存不改变已提交状态，正式提交更新提交时间
        String status;
        LocalDateTime submittedAt;
        if (submit) {
            status = "SUBMITTED";
            submittedAt = LocalDateTime.now();
        }
        else if ("SUBMITTED".equals(existing.getStatus())) {
            status = "SUBMITTED";
            submittedAt = existing.getSubmittedAt();
        }
        else {
            status = "DRAFT";
            submittedAt = null;
        }

        existing.setProgressText(StringUtils.trim(req.getProgressText()));
        existing.setWorkDays(req.getWorkDays());
        existing.setBlockers(StringUtils.trimToNull(req.getBlockers()));
        existing.setNextPlan(StringUtils.trimToNull(req.getNextPlan()));
        existing.setStatus(status);
        existing.setSubmittedAt(submittedAt);
        userItemMapper.updateById(existing);
        return existing.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long upsertProjectMonthReport(ProjectMonthReportUpsertRequest req) {
        if (req == null) throw new IllegalArgumentException("参数不能为空");
        if (StringUtils.isBlank(req.getReportYm())) throw new IllegalArgumentException("reportYm不能为空");
        if (req.getProjectId() == null) throw new IllegalArgumentException("projectId不能为空");
        if (StringUtils.isBlank(req.getSummaryText())) throw new IllegalArgumentException("summaryText不能为空");

        YearMonth ym = parseYm(req.getReportYm());
        Long tenantId = tenantOrDefault();
        Long uid = requireUserId();

        DeptDailyProject p = projectMapper.selectOne(new LambdaQueryWrapper<DeptDailyProject>()
                .eq(DeptDailyProject::getTenantId, tenantId)
                .eq(DeptDailyProject::getId, req.getProjectId())
                .last("limit 1"));
        if (p == null) throw new IllegalArgumentException("项目不存在");

        // 权限：负责人 / 超管 / 租户管理员 / 权限标识
        boolean can = uid.equals(p.getLeaderUserId())
                || SessionHelper.isAdmin()
                || SessionHelper.isTenantAdmin()
                || SessionHelper.hasPermission("dept-daily:project:monthly-report");
        if (!can) throw new IllegalStateException("无权限操作");

        DeptDailyProjectMonthReport existing = projectReportMapper.selectOne(new LambdaQueryWrapper<DeptDailyProjectMonthReport>()
                .eq(DeptDailyProjectMonthReport::getTenantId, tenantId)
                .eq(DeptDailyProjectMonthReport::getReportYm, ym.format(YM_FMT))
                .eq(DeptDailyProjectMonthReport::getProjectId, req.getProjectId())
                .last("limit 1"));

        boolean submit = Boolean.TRUE.equals(req.getSubmit());

        if (existing == null) {
            String status = submit ? "SUBMITTED" : "DRAFT";
            LocalDateTime submittedAt = submit ? LocalDateTime.now() : null;
            DeptDailyProjectMonthReport r = new DeptDailyProjectMonthReport();
            r.setTenantId(tenantId);
            r.setDeptId(p.getDeptId());
            r.setOfficeId(p.getOfficeId());
            r.setReportYm(ym.format(YM_FMT));
            r.setProjectId(req.getProjectId());
            r.setSummaryText(StringUtils.trim(req.getSummaryText()));
            r.setOverallStatus(StringUtils.trimToNull(req.getOverallStatus()));
            r.setRisks(StringUtils.trimToNull(req.getRisks()));
            r.setNextPlan(StringUtils.trimToNull(req.getNextPlan()));
            r.setStatus(status);
            r.setSubmittedAt(submittedAt);
            projectReportMapper.insert(r);
            return r.getId();
        }

        String status;
        LocalDateTime submittedAt;
        if (submit) {
            status = "SUBMITTED";
            submittedAt = LocalDateTime.now();
        }
        else if ("SUBMITTED".equals(existing.getStatus())) {
            status = "SUBMITTED";
            submittedAt = existing.getSubmittedAt();
        }
        else {
            status = "DRAFT";
            submittedAt = null;
        }

        existing.setSummaryText(StringUtils.trim(req.getSummaryText()));
        existing.setOverallStatus(StringUtils.trimToNull(req.getOverallStatus()));
        existing.setRisks(StringUtils.trimToNull(req.getRisks()));
        existing.setNextPlan(StringUtils.trimToNull(req.getNextPlan()));
        existing.setStatus(status);
        existing.setSubmittedAt(submittedAt);
        projectReportMapper.updateById(existing);
        return existing.getId();
    }

    private static YearMonth parseYm(String reportYm) {
        try {
            return YearMonth.parse(reportYm, YM_FMT);
        } catch (Exception e) {
            throw new IllegalArgumentException("reportYm格式错误，应为YYYY-MM");
        }
    }

    private void upsertUserSheet(Long tenantId, Long uid, Long deptId, Long officeId, String reportYm, boolean submit) {
        Integer employeeType = null;
        DeptDailySysUserLite u = userLiteMapper.selectOne(new LambdaQueryWrapper<DeptDailySysUserLite>()
                .eq(DeptDailySysUserLite::getTenantId, tenantId)
                .eq(DeptDailySysUserLite::getId, uid)
                .last("limit 1"));
        if (u != null) {
            employeeType = u.getEmployeeType();
        }

        DeptDailyUserMonthReportSheet sheet = userSheetMapper.selectOne(new LambdaQueryWrapper<DeptDailyUserMonthReportSheet>()
                .eq(DeptDailyUserMonthReportSheet::getTenantId, tenantId)
                .eq(DeptDailyUserMonthReportSheet::getReportYm, reportYm)
                .eq(DeptDailyUserMonthReportSheet::getUserId, uid)
                .last("limit 1"));
        if (sheet == null) {
            DeptDailyUserMonthReportSheet s = new DeptDailyUserMonthReportSheet();
            s.setTenantId(tenantId);
            s.setDeptId(deptId);
            s.setOfficeId(officeId);
            s.setEmployeeType(employeeType);
            s.setReportYm(reportYm);
            s.setUserId(uid);
            if (submit) {
                s.setStatus("SUBMITTED");
                s.setSubmittedAt(LocalDateTime.now());
            } else {
                s.setStatus("DRAFT");
                s.setSubmittedAt(null);
            }
            userSheetMapper.insert(s);
            return;
        }

        // 当月总表已为「已提交」时仍允许同步各项目明细（自动保存）；仅在有新的正式提交时再刷新 submittedAt
        if (employeeType != null && (sheet.getEmployeeType() == null || !employeeType.equals(sheet.getEmployeeType()))) {
            sheet.setEmployeeType(employeeType);
        }
        if (submit) {
            sheet.setStatus("SUBMITTED");
            sheet.setSubmittedAt(LocalDateTime.now());
        } else if (!"SUBMITTED".equals(sheet.getStatus())) {
            sheet.setStatus("DRAFT");
        }
        userSheetMapper.updateById(sheet);
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

