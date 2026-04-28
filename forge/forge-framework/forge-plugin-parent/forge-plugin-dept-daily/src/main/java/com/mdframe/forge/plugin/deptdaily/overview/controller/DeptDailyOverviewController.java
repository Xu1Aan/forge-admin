package com.mdframe.forge.plugin.deptdaily.overview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.deptdaily.overview.dto.FillStateRefreshRequest;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailyReportSetting;
import com.mdframe.forge.plugin.deptdaily.overview.service.DeptDailyOverviewService;
import com.mdframe.forge.plugin.deptdaily.overview.vo.FillStateRowVO;
import com.mdframe.forge.plugin.deptdaily.overview.vo.ProjectProgressRowVO;
import com.mdframe.forge.plugin.deptdaily.overview.vo.UserMonthReportStatRowVO;
import com.mdframe.forge.plugin.deptdaily.overview.vo.AttendanceMonthTableRowVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 统览接口（按年月、部门/科室、员工类型筛选）
 * - 考勤统览：未填报列表等（明细统计可后续加专用统计SQL）
 * - 月报统览：项目进展表、员工月报统计表
 */
@RestController
@RequestMapping({"/dept-daily/overview", "/api/dept-daily/overview"})
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class DeptDailyOverviewController {

    private final DeptDailyOverviewService overviewService;
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 统览起始月份设置（按 scope 唯一：tenant + dept + office + employeeType）
     */
    @GetMapping("/setting")
    public RespInfo<DeptDailyReportSetting> getSetting(@RequestParam(required = false) Long deptId,
                                                       @RequestParam(required = false) Long officeId,
                                                       @RequestParam(required = false) Integer employeeType) {
        return RespInfo.success(overviewService.getSetting(deptId, officeId, employeeType));
    }

    @PostMapping("/setting")
    public RespInfo<Void> saveSetting(@RequestBody DeptDailyReportSetting req) {
        overviewService.saveSetting(req);
        return RespInfo.success();
    }

    /**
     * 未填报/填报状态分页（统一入口）
     * module: ATTENDANCE / WORK_REPORT / PROJECT_REPORT
     * status: NONE / DRAFT / SUBMITTED
     */
    @GetMapping("/fill-state/page")
    public RespInfo<IPage<FillStateRowVO>> pageFillState(PageQuery pageQuery,
                                                         @RequestParam String module,
                                                         @RequestParam String ym,
                                                         @RequestParam(required = false) Long deptId,
                                                         @RequestParam(required = false) Long officeId,
                                                         @RequestParam(required = false) Integer employeeType,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String keyword) {
        return RespInfo.success(overviewService.pageFillState(
                pageQuery,
                module,
                ym,
                deptId,
                officeId,
                employeeType,
                status,
                keyword
        ));
    }

    /**
     * 月报统览：项目进展表（项目名、负责人、进展情况、操作=reportId可用于详情）
     */
    @GetMapping("/report/project-progress/page")
    public RespInfo<IPage<ProjectProgressRowVO>> pageProjectProgress(PageQuery pageQuery,
                                                                     @RequestParam String reportYm,
                                                                     @RequestParam(required = false) Long deptId,
                                                                     @RequestParam(required = false) Long officeId,
                                                                     @RequestParam(required = false) String keyword) {
        return RespInfo.success(overviewService.pageProjectProgress(pageQuery, reportYm, deptId, officeId, keyword));
    }

    /**
     * 月报统览：员工月报统计表（可按人员搜索；点击人员可再走明细接口：/dept-daily/report/user-month-item/page）
     */
    @GetMapping("/report/user/page")
    public RespInfo<IPage<UserMonthReportStatRowVO>> pageUserMonthReportStat(PageQuery pageQuery,
                                                                             @RequestParam String reportYm,
                                                                             @RequestParam(required = false) Long deptId,
                                                                             @RequestParam(required = false) Long officeId,
                                                                             @RequestParam(required = false) Integer employeeType,
                                                                             @RequestParam(required = false) String status,
                                                                             @RequestParam(required = false) String keyword) {
        return RespInfo.success(overviewService.pageUserMonthReportStat(
                pageQuery, reportYm, deptId, officeId, employeeType, status, keyword
        ));
    }

    /**
     * 考勤一览表：按月分页展示人员 1..31 每日情况 + 汇总。
     */
    @GetMapping("/attendance/month-table/page")
    public RespInfo<IPage<AttendanceMonthTableRowVO>> pageAttendanceMonthTable(PageQuery pageQuery,
                                                                              @RequestParam int year,
                                                                              @RequestParam int month,
                                                                              @RequestParam(required = false) Long deptId,
                                                                              @RequestParam(required = false) Long officeId,
                                                                              @RequestParam(required = false) Integer employeeType,
                                                                              @RequestParam(required = false) String keyword) {
        return RespInfo.success(overviewService.pageAttendanceMonthTable(
                pageQuery, year, month, deptId, officeId, employeeType, keyword
        ));
    }

    /**
     * 手动批量刷新 fill_state（按起始月到结束月）
     * - startYm 为空：从 setting 取对应模块的 start_ym；再为空则默认当前月
     * - endYm 为空：默认当前月（且不允许超过当前月）
     */
    @PostMapping("/fill-state/refresh")
    public RespInfo<Integer> refreshFillState(@RequestBody FillStateRefreshRequest req) {
        if (req == null || req.getModule() == null) {
            return RespInfo.error("module不能为空");
        }
        DeptDailyReportSetting setting = overviewService.getSetting(req.getDeptId(), req.getOfficeId(), req.getEmployeeType());
        String startYm = req.getStartYm();
        if (startYm == null || startYm.isBlank()) {
            if (setting != null) {
                if ("ATTENDANCE".equalsIgnoreCase(req.getModule())) startYm = setting.getAttendanceStartYm();
                if ("WORK_REPORT".equalsIgnoreCase(req.getModule())) startYm = setting.getWorkReportStartYm();
                if ("PROJECT_REPORT".equalsIgnoreCase(req.getModule())) startYm = setting.getProjectReportStartYm();
            }
        }
        if (startYm == null || startYm.isBlank()) {
            startYm = YearMonth.now().format(YM_FMT);
        }
        String endYm = req.getEndYm();
        if (endYm == null || endYm.isBlank()) {
            endYm = YearMonth.now().format(YM_FMT);
        }
        int n = overviewService.refreshFillState(req.getModule(), req.getDeptId(), req.getOfficeId(), req.getEmployeeType(), startYm, endYm);
        return RespInfo.success(n);
    }
}

