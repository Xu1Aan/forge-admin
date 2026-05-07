package com.mdframe.forge.plugin.deptdaily.overview.service;

import com.mdframe.forge.plugin.deptdaily.attendance.enums.AttendanceDayStatus;
import com.mdframe.forge.plugin.deptdaily.attendance.enums.LeaveType;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailySysOrgLite;
import com.mdframe.forge.plugin.deptdaily.overview.mapper.DeptDailySysOrgLiteMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeptDailyAttendanceExportService {

    private final DeptDailyOverviewService overviewService;
    private final DeptDailySysOrgLiteMapper orgLiteMapper;

    public void export(HttpServletResponse response, int year, Integer month,
                       Long deptId, Long officeId, Integer employeeType, String keyword) {
        if (month == null) {
            exportYear(response, year, deptId, officeId, employeeType, keyword);
            return;
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month必须在1~12之间");
        }

        try (Workbook wb = new XSSFWorkbook()) {
            String deptName = resolveDeptName(year, deptId, officeId);
            buildMonthSheet(wb, year, month, deptName, deptId, officeId, employeeType, keyword);

            String fileName = String.format("%d-%02d考勤表.xlsx", year, month);
            setResponseHeaders(response, fileName);
            wb.write(response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    public void exportYear(HttpServletResponse response, int year,
                           Long deptId, Long officeId, Integer employeeType, String keyword) {
        try (Workbook wb = new XSSFWorkbook()) {
            String deptName = resolveDeptName(year, deptId, officeId);
            for (int month = 1; month <= 12; month++) {
                buildMonthSheet(wb, year, month, deptName, deptId, officeId, employeeType, keyword);
            }

            String fileName = year + "年度考勤表.xlsx";
            setResponseHeaders(response, fileName);
            wb.write(response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    private String resolveDeptName(int year, Long deptId, Long officeId) {
        Long orgId = officeId != null ? officeId : deptId;
        if (orgId == null && !SessionHelper.isAdmin() && !SessionHelper.isTenantAdmin()) {
            orgId = SessionHelper.getMainOrgId();
        }
        if (orgId == null) return "未指定部门";
        DeptDailySysOrgLite org = orgLiteMapper.selectById(orgId);
        if (org != null && StringUtils.isNotBlank(org.getOrgName())) return org.getOrgName();
        return "部门(" + orgId + ")";
    }

    private void buildMonthSheet(Workbook wb, int year, int month, String deptName,
                                 Long deptId, Long officeId, Integer employeeType, String keyword) {
        String sheetName = String.format("%04d.%02d", year, month);
        Sheet sheet = wb.createSheet(sheetName);
        sheet.setDefaultRowHeightInPoints(18f);

        Map<String, CellStyle> styles = buildStyles(wb);
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();

        // Column widths (approx)
        sheet.setColumnWidth(0, 6 * 256);
        sheet.setColumnWidth(1, 12 * 256);
        sheet.setColumnWidth(2, 8 * 256);
        sheet.setColumnWidth(3, 3 * 256);
        for (int c = 4; c <= 34; c++) sheet.setColumnWidth(c, 3 * 256);
        for (int c = 35; c <= 37; c++) sheet.setColumnWidth(c, 6 * 256);
        sheet.setColumnWidth(38, 10 * 256);

        // Row 0: 附1
        Row r0 = sheet.createRow(0);
        createCell(r0, 0, "附1", styles.get("titleLeft"));

        // Row 2: 标题
        Row r2 = sheet.createRow(2);
        createCell(r2, 0, "四川水发勘测设计研究有限公司人员月度考勤表", styles.get("title"));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 38));

        // Row 3: 部门
        Row r3 = sheet.createRow(3);
        createCell(r3, 0, "部门：" + deptName, styles.get("subTitleLeft"));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(3, 3, 0, 10));

        // Row 4: 表头（序号/姓名/日期 + 1..31 + 合计）
        Row r4 = sheet.createRow(4);
        createCell(r4, 0, "序号", styles.get("header"));
        createCell(r4, 1, "姓名", styles.get("header"));
        createCell(r4, 2, "日期", styles.get("header"));
        createCell(r4, 3, "", styles.get("header"));
        for (int d = 1; d <= 31; d++) {
            createCell(r4, 3 + d, d, styles.get("header"));
        }
        createCell(r4, 35, "合计", styles.get("header"));
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(4, 4, 35, 37));
        createCell(r4, 38, "填报状态", styles.get("header"));

        // Row 5: 星期 + 统计列
        Row r5 = sheet.createRow(5);
        createCell(r5, 2, "星期", styles.get("header"));
        createCell(r5, 3, "", styles.get("header"));
        for (int d = 1; d <= 31; d++) {
            String wd = d <= daysInMonth ? weekdayCn(LocalDate.of(year, month, d).getDayOfWeek()) : "";
            createCell(r5, 3 + d, wd, styles.get("header"));
        }
        createCell(r5, 35, "出勤", styles.get("header"));
        createCell(r5, 36, "出差", styles.get("header"));
        createCell(r5, 37, "休假", styles.get("header"));
        createCell(r5, 38, "", styles.get("header"));

        // Data rows start at row 6
        List<DeptDailyOverviewService.AttendanceMonthExportUserRow> users =
                overviewService.listAttendanceMonthExportRows(year, month, deptId, officeId, employeeType, keyword);

        int rowIdx = 6;
        int seq = 1;
        for (DeptDailyOverviewService.AttendanceMonthExportUserRow u : users) {
            Row r = sheet.createRow(rowIdx++);
            createCell(r, 0, seq++, styles.get("cellCenter"));
            createCell(r, 1, StringUtils.defaultIfBlank(u.getRealName(), u.getUsername()), styles.get("cellLeft"));
            createCell(r, 2, "", styles.get("cellCenter"));
            createCell(r, 3, "", styles.get("cellCenter"));

            for (int d = 1; d <= 31; d++) {
                String sym = "";
                if (u.getDays() != null && u.getDays().size() >= d) {
                    var eff = u.getDays().get(d - 1);
                    sym = statusToSymbol(eff.dayStatus(), eff.leaveType());
                }
                createCell(r, 3 + d, sym, styles.get("cellCenter"));
            }

            int work = nvl(u.getWorkDays());
            int travel = nvl(u.getTravelDays());
            double restLeave = nvl(u.getRestDays()) + nvlD(u.getLeaveDays());
            createCell(r, 35, work, styles.get("cellCenter"));
            createCell(r, 36, travel, styles.get("cellCenter"));
            createCell(r, 37, restLeave, styles.get("cellCenter"));
            createCell(r, 38, "NONE".equalsIgnoreCase(u.getSheetStatus()) ? "未填写" : "", styles.get("cellCenter"));
        }

        // grid borders for header + data
        int lastRow = Math.max(5, rowIdx - 1);
        for (int r = 4; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c <= 38; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) {
                    cell = row.createCell(c);
                    cell.setCellStyle(styles.get(r <= 5 ? "header" : "cellCenter"));
                }
            }
        }

        sheet.createFreezePane(2, 6);
    }

    private static int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    private static double nvlD(Double v) {
        return v == null ? 0d : v;
    }

    private static String statusToSymbol(String dayStatus, String leaveType) {
        if (AttendanceDayStatus.WORK.equals(dayStatus)) return "/";
        if (AttendanceDayStatus.TRAVEL.equals(dayStatus)) return "√";
        if (AttendanceDayStatus.REST.equals(dayStatus)) return "休";
        if (AttendanceDayStatus.LEAVE.equals(dayStatus)) {
            if (LeaveType.SICK.equals(leaveType)) return "病";
            if (LeaveType.PERSONAL.equals(leaveType)) return "事";
            if (LeaveType.PUBLIC.equals(leaveType)) return "休";
            if (LeaveType.MARRIAGE.equals(leaveType)) return "婚";
            if (LeaveType.BEREAVEMENT.equals(leaveType)) return "丧";
            if (LeaveType.ANNUAL.equals(leaveType)) return "年";
            if (LeaveType.MATERNITY.equals(leaveType)) return "产";
            if (LeaveType.FAMILY_PLANNING.equals(leaveType)) return "产";
            if (LeaveType.NURSING_CARE.equals(leaveType)) return "产";
            if (LeaveType.HOME_VISIT.equals(leaveType)) return "探";
            if (LeaveType.WORK_INJURY.equals(leaveType)) return "伤";
            return "假";
        }
        return "";
    }

    private static String weekdayCn(DayOfWeek w) {
        if (w == null) return "";
        return switch (w) {
            case MONDAY -> "一";
            case TUESDAY -> "二";
            case WEDNESDAY -> "三";
            case THURSDAY -> "四";
            case FRIDAY -> "五";
            case SATURDAY -> "六";
            case SUNDAY -> "日";
        };
    }

    private static Map<String, CellStyle> buildStyles(Workbook wb) {
        Map<String, CellStyle> m = new HashMap<>();

        Font titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);

        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 10);

        Font normalFont = wb.createFont();
        normalFont.setFontHeightInPoints((short) 10);

        CellStyle title = wb.createCellStyle();
        title.setAlignment(HorizontalAlignment.CENTER);
        title.setVerticalAlignment(VerticalAlignment.CENTER);
        title.setFont(titleFont);
        m.put("title", title);

        CellStyle titleLeft = wb.createCellStyle();
        titleLeft.setAlignment(HorizontalAlignment.LEFT);
        titleLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        titleLeft.setFont(headerFont);
        m.put("titleLeft", titleLeft);

        CellStyle subTitleLeft = wb.createCellStyle();
        subTitleLeft.setAlignment(HorizontalAlignment.LEFT);
        subTitleLeft.setVerticalAlignment(VerticalAlignment.CENTER);
        subTitleLeft.setFont(normalFont);
        m.put("subTitleLeft", subTitleLeft);

        CellStyle header = wb.createCellStyle();
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        header.setFont(headerFont);
        header.setBorderBottom(BorderStyle.THIN);
        header.setBorderTop(BorderStyle.THIN);
        header.setBorderLeft(BorderStyle.THIN);
        header.setBorderRight(BorderStyle.THIN);
        header.setWrapText(true);
        m.put("header", header);

        CellStyle cellCenter = wb.createCellStyle();
        cellCenter.setAlignment(HorizontalAlignment.CENTER);
        cellCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        cellCenter.setFont(normalFont);
        cellCenter.setBorderBottom(BorderStyle.THIN);
        cellCenter.setBorderTop(BorderStyle.THIN);
        cellCenter.setBorderLeft(BorderStyle.THIN);
        cellCenter.setBorderRight(BorderStyle.THIN);
        m.put("cellCenter", cellCenter);

        CellStyle cellLeft = wb.createCellStyle();
        cellLeft.cloneStyleFrom(cellCenter);
        cellLeft.setAlignment(HorizontalAlignment.LEFT);
        m.put("cellLeft", cellLeft);

        return m;
    }

    private static void createCell(Row row, int col, String v, CellStyle style) {
        Cell c = row.createCell(col, CellType.STRING);
        c.setCellValue(v != null ? v : "");
        if (style != null) c.setCellStyle(style);
    }

    private static void createCell(Row row, int col, int v, CellStyle style) {
        Cell c = row.createCell(col, CellType.NUMERIC);
        c.setCellValue(v);
        if (style != null) c.setCellStyle(style);
    }

    private static void createCell(Row row, int col, double v, CellStyle style) {
        Cell c = row.createCell(col, CellType.NUMERIC);
        c.setCellValue(v);
        if (style != null) c.setCellStyle(style);
    }

    private static void setResponseHeaders(HttpServletResponse response, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encoded);
    }
}

