package com.mdframe.forge.plugin.deptdaily.overview.controller;

import com.mdframe.forge.plugin.deptdaily.overview.service.DeptDailyAttendanceExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 考勤一览表导出（Excel下载）
 *
 * <p>注意：此接口为文件下载，不走 ApiEncrypt/ApiDecrypt。</p>
 */
@RestController
@RequestMapping({"/api/dept-daily/overview/attendance", "/dept-daily/overview/attendance"})
@RequiredArgsConstructor
public class DeptDailyAttendanceExportController {

    private final DeptDailyAttendanceExportService exportService;

    /**
     * 导出年度考勤表（12个sheet：yyyy.MM）
     */
    @GetMapping("/year-export")
    public void exportYear(HttpServletResponse response,
                           @RequestParam int year,
                           @RequestParam(required = false) Long deptId,
                           @RequestParam(required = false) Long officeId,
                           @RequestParam(required = false) Integer employeeType,
                           @RequestParam(required = false) String keyword) {
        exportService.exportYear(response, year, deptId, officeId, employeeType, keyword);
    }

    /**
     * 导出考勤表：
     * - month 为空：导出全年（12个sheet）
     * - month 非空：导出当月（单sheet）
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam int year,
                       @RequestParam(required = false) Integer month,
                       @RequestParam(required = false) Long deptId,
                       @RequestParam(required = false) Long officeId,
                       @RequestParam(required = false) Integer employeeType,
                       @RequestParam(required = false) String keyword) {
        exportService.export(response, year, month, deptId, officeId, employeeType, keyword);
    }
}

