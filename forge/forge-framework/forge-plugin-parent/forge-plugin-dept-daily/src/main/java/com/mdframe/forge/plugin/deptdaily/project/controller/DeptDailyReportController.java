package com.mdframe.forge.plugin.deptdaily.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectMonthReportUpsertRequest;
import com.mdframe.forge.plugin.deptdaily.project.dto.UserMonthReportUpsertRequest;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProjectMonthReport;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyUserMonthReportItem;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyProjectMonthReportMapper;
import com.mdframe.forge.plugin.deptdaily.project.mapper.DeptDailyUserMonthReportItemMapper;
import com.mdframe.forge.plugin.deptdaily.project.service.DeptDailyReportService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/dept-daily/report", "/api/dept-daily/report"})
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class DeptDailyReportController {

    private final DeptDailyReportService reportService;
    private final DeptDailyUserMonthReportItemMapper userItemMapper;
    private final DeptDailyProjectMonthReportMapper projectReportMapper;

    @PostMapping("/user-month-item")
    public RespInfo<Long> upsertUserMonthItem(@RequestBody UserMonthReportUpsertRequest req) {
        return RespInfo.success(reportService.upsertUserMonthItem(req));
    }

    @GetMapping("/user-month-item/page")
    public RespInfo<IPage<DeptDailyUserMonthReportItem>> pageUserMonthItem(PageQuery pageQuery,
                                                                           @RequestParam String reportYm) {
        Long tenantId = SessionHelper.getTenantId() != null ? SessionHelper.getTenantId() : 1L;
        Long uid = SessionHelper.getUserId();
        if (uid == null) return RespInfo.error("未登录");
        if (StringUtils.isBlank(reportYm)) return RespInfo.error("reportYm不能为空");

        IPage<DeptDailyUserMonthReportItem> page = userItemMapper.selectPage(pageQuery.toPage(),
                new LambdaQueryWrapper<DeptDailyUserMonthReportItem>()
                        .eq(DeptDailyUserMonthReportItem::getTenantId, tenantId)
                        .eq(DeptDailyUserMonthReportItem::getUserId, uid)
                        .eq(DeptDailyUserMonthReportItem::getReportYm, reportYm)
                        .orderByDesc(DeptDailyUserMonthReportItem::getUpdateTime)
                        .orderByDesc(DeptDailyUserMonthReportItem::getId));
        return RespInfo.success(page);
    }

    /**
     * 统览/领导视角：查询指定用户某月的月报明细
     * 权限：超管/租户管理员/拥有权限标识 dept-daily:report:overview
     */
    @GetMapping("/user-month-item/page-by-user")
    public RespInfo<IPage<DeptDailyUserMonthReportItem>> pageUserMonthItemByUser(PageQuery pageQuery,
                                                                                 @RequestParam String reportYm,
                                                                                 @RequestParam Long userId) {
        Long tenantId = SessionHelper.getTenantId() != null ? SessionHelper.getTenantId() : 1L;
        Long uid = SessionHelper.getUserId();
        if (uid == null) return RespInfo.error("未登录");
        if (StringUtils.isBlank(reportYm)) return RespInfo.error("reportYm不能为空");
        if (userId == null) return RespInfo.error("userId不能为空");

        boolean can = SessionHelper.isAdmin()
                || SessionHelper.isTenantAdmin()
                || SessionHelper.hasPermission("dept-daily:report:overview");
        if (!can) return RespInfo.error("无权限");

        IPage<DeptDailyUserMonthReportItem> page = userItemMapper.selectPage(pageQuery.toPage(),
                new LambdaQueryWrapper<DeptDailyUserMonthReportItem>()
                        .eq(DeptDailyUserMonthReportItem::getTenantId, tenantId)
                        .eq(DeptDailyUserMonthReportItem::getUserId, userId)
                        .eq(DeptDailyUserMonthReportItem::getReportYm, reportYm)
                        .orderByDesc(DeptDailyUserMonthReportItem::getUpdateTime)
                        .orderByDesc(DeptDailyUserMonthReportItem::getId));
        return RespInfo.success(page);
    }

    @PostMapping("/project-month")
    public RespInfo<Long> upsertProjectMonth(@RequestBody ProjectMonthReportUpsertRequest req) {
        return RespInfo.success(reportService.upsertProjectMonthReport(req));
    }

    @GetMapping("/project-month/page")
    public RespInfo<IPage<DeptDailyProjectMonthReport>> pageProjectMonth(PageQuery pageQuery,
                                                                         @RequestParam String reportYm,
                                                                         @RequestParam Long projectId) {
        Long tenantId = SessionHelper.getTenantId() != null ? SessionHelper.getTenantId() : 1L;
        if (StringUtils.isBlank(reportYm)) return RespInfo.error("reportYm不能为空");
        if (projectId == null) return RespInfo.error("projectId不能为空");
        IPage<DeptDailyProjectMonthReport> page = projectReportMapper.selectPage(pageQuery.toPage(),
                new LambdaQueryWrapper<DeptDailyProjectMonthReport>()
                        .eq(DeptDailyProjectMonthReport::getTenantId, tenantId)
                        .eq(DeptDailyProjectMonthReport::getReportYm, reportYm)
                        .eq(DeptDailyProjectMonthReport::getProjectId, projectId)
                        .orderByDesc(DeptDailyProjectMonthReport::getUpdateTime)
                        .orderByDesc(DeptDailyProjectMonthReport::getId));
        return RespInfo.success(page);
    }
}

