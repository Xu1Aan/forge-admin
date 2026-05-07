package com.mdframe.forge.plugin.deptdaily.overview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.deptdaily.attendance.entity.DeptAttendanceItem;
import com.mdframe.forge.plugin.deptdaily.attendance.entity.DeptAttendanceSheet;
import com.mdframe.forge.plugin.deptdaily.attendance.enums.AttendanceDayStatus;
import com.mdframe.forge.plugin.deptdaily.attendance.mapper.DeptAttendanceItemMapper;
import com.mdframe.forge.plugin.deptdaily.attendance.mapper.DeptAttendanceSheetMapper;
import com.mdframe.forge.plugin.deptdaily.attendance.service.DeptCalendarService;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailyReportSetting;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailyFillState;
import com.mdframe.forge.plugin.deptdaily.overview.mapper.DeptDailySysOrgLiteMapper;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailySysUserLite;
import com.mdframe.forge.plugin.deptdaily.overview.mapper.DeptDailyFillStateMapper;
import com.mdframe.forge.plugin.deptdaily.overview.mapper.DeptDailyProjectReportOverviewMapper;
import com.mdframe.forge.plugin.deptdaily.overview.mapper.DeptDailyReportSettingMapper;
import com.mdframe.forge.plugin.deptdaily.overview.mapper.DeptDailySysUserLiteMapper;
import com.mdframe.forge.plugin.deptdaily.overview.mapper.DeptDailySysUserOrgLiteMapper;
import com.mdframe.forge.plugin.deptdaily.overview.mapper.DeptDailyUserMonthReportSheetMapper;
import com.mdframe.forge.plugin.deptdaily.overview.vo.AttendanceMonthTableRowVO;
import com.mdframe.forge.plugin.deptdaily.overview.vo.FillStateRowVO;
import com.mdframe.forge.plugin.deptdaily.overview.vo.ProjectProgressRowVO;
import com.mdframe.forge.plugin.deptdaily.overview.vo.UserMonthReportStatRowVO;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DeptDailyOverviewService {

    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final DeptDailyReportSettingMapper settingMapper;
    private final DeptDailyFillStateMapper fillStateMapper;
    private final DeptDailyUserMonthReportSheetMapper userSheetMapper;
    private final DeptDailyProjectReportOverviewMapper projectProgressMapper;
    private final DeptDailySysUserLiteMapper userLiteMapper;
    private final DeptDailySysOrgLiteMapper orgLiteMapper;
    private final DeptDailySysUserOrgLiteMapper userOrgLiteMapper;
    private final DeptAttendanceSheetMapper attendanceSheetMapper;
    private final DeptAttendanceItemMapper attendanceItemMapper;
    private final DeptCalendarService calendarService;

    public DeptDailyReportSetting getSetting(Long deptId, Long officeId, Integer employeeType) {
        Long tenantId = tenantOrDefault();
        return settingMapper.selectOne(new LambdaQueryWrapper<DeptDailyReportSetting>()
                .eq(DeptDailyReportSetting::getTenantId, tenantId)
                .eq(deptId != null, DeptDailyReportSetting::getDeptId, deptId)
                .isNull(deptId == null, DeptDailyReportSetting::getDeptId)
                .eq(officeId != null, DeptDailyReportSetting::getOfficeId, officeId)
                .isNull(officeId == null, DeptDailyReportSetting::getOfficeId)
                .eq(employeeType != null, DeptDailyReportSetting::getEmployeeType, employeeType)
                .isNull(employeeType == null, DeptDailyReportSetting::getEmployeeType)
                .last("limit 1"));
    }

    public void saveSetting(DeptDailyReportSetting req) {
        if (req == null) throw new IllegalArgumentException("参数不能为空");
        Long tenantId = tenantOrDefault();

        DeptDailyReportSetting existing = getSetting(req.getDeptId(), req.getOfficeId(), req.getEmployeeType());
        if (existing == null) {
            DeptDailyReportSetting s = new DeptDailyReportSetting();
            s.setTenantId(tenantId);
            s.setDeptId(req.getDeptId());
            s.setOfficeId(req.getOfficeId());
            s.setEmployeeType(req.getEmployeeType());
            s.setAttendanceStartYm(StringUtils.trimToNull(req.getAttendanceStartYm()));
            s.setWorkReportStartYm(StringUtils.trimToNull(req.getWorkReportStartYm()));
            s.setProjectReportStartYm(StringUtils.trimToNull(req.getProjectReportStartYm()));
            s.setAttendanceExportOrder(StringUtils.trimToNull(req.getAttendanceExportOrder()));
            settingMapper.insert(s);
            return;
        }
        existing.setAttendanceStartYm(StringUtils.trimToNull(req.getAttendanceStartYm()));
        existing.setWorkReportStartYm(StringUtils.trimToNull(req.getWorkReportStartYm()));
        existing.setProjectReportStartYm(StringUtils.trimToNull(req.getProjectReportStartYm()));
        existing.setAttendanceExportOrder(StringUtils.trimToNull(req.getAttendanceExportOrder()));
        settingMapper.updateById(existing);
    }

    public IPage<FillStateRowVO> pageFillState(PageQuery pageQuery, String module, String ym,
                                               Long deptId, Long officeId, Integer employeeType,
                                               String status, String keyword) {
        Long tenantId = tenantOrDefault();
        return fillStateMapper.selectFillStatePage(
                pageQuery.toPage(),
                tenantId,
                module,
                ym,
                deptId,
                officeId,
                employeeType,
                StringUtils.trimToNull(status),
                StringUtils.trimToNull(keyword)
        );
    }

    public IPage<ProjectProgressRowVO> pageProjectProgress(PageQuery pageQuery, String reportYm,
                                                           Long deptId, Long officeId, String keyword,
                                                           String projectCategory) {
        Long tenantId = tenantOrDefault();
        return projectProgressMapper.selectProjectProgressPage(
                pageQuery.toPage(),
                tenantId,
                reportYm,
                deptId,
                officeId,
                StringUtils.trimToNull(keyword),
                StringUtils.trimToNull(projectCategory)
        );
    }

    public IPage<UserMonthReportStatRowVO> pageUserMonthReportStat(PageQuery pageQuery, String reportYm,
                                                                   Long deptId, Long officeId,
                                                                   Integer employeeType, String status,
                                                                   String keyword) {
        Long tenantId = tenantOrDefault();
        return userSheetMapper.selectUserMonthReportStatPage(
                pageQuery.toPage(),
                tenantId,
                reportYm,
                deptId,
                officeId,
                employeeType,
                StringUtils.trimToNull(status),
                StringUtils.trimToNull(keyword)
        );
    }

    /**
     * 考勤一览表：本部门及下属部门（默认主组织）人员的按月明细（1..31）+ 汇总统计。
     */
    public IPage<AttendanceMonthTableRowVO> pageAttendanceMonthTable(PageQuery pageQuery, int year, int month,
                                                                     Long deptId, Long officeId,
                                                                     Integer employeeType, String keyword) {
        Long tenantId = tenantOrDefault();

        LambdaQueryWrapper<DeptDailySysUserLite> uw = new LambdaQueryWrapper<DeptDailySysUserLite>()
                .eq(DeptDailySysUserLite::getTenantId, tenantId)
                .eq(DeptDailySysUserLite::getUserStatus, 1)
                .eq(employeeType != null, DeptDailySysUserLite::getEmployeeType, employeeType);

        // 组织范围：优先用 sys_user_org（支持多组织），回落到 sys_user.create_dept
        List<Long> scopeOrgIds = resolveScopeOrgIds(tenantId, deptId, officeId);
        List<Long> scopeUserIds = resolveScopeUserIds(tenantId, scopeOrgIds);
        if (scopeUserIds != null) {
            if (scopeUserIds.isEmpty()) {
                Page<AttendanceMonthTableRowVO> empty = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize(), 0);
                empty.setRecords(List.of());
                return empty;
            }
            uw.in(DeptDailySysUserLite::getId, scopeUserIds);
        } else {
            // 若未能拿到 sys_user_org 数据（兼容老数据/极端情况），使用 create_dept 过滤
            Long scopeOrgId = resolveScopeOrgId(deptId, officeId);
            if (scopeOrgId != null) {
                List<Long> descendant = orgLiteMapper.selectDescendantOrgIds(tenantId, scopeOrgId);
                if (descendant != null && !descendant.isEmpty()) {
                    uw.in(DeptDailySysUserLite::getCreateDept, descendant);
                } else {
                    uw.eq(DeptDailySysUserLite::getCreateDept, scopeOrgId);
                }
            }
        }

        String kw = StringUtils.trimToNull(keyword);
        if (kw != null) {
            uw.and(w -> w.like(DeptDailySysUserLite::getUsername, kw).or().like(DeptDailySysUserLite::getRealName, kw));
        }
        uw.orderByAsc(DeptDailySysUserLite::getId);

        Page<DeptDailySysUserLite> userPage = userLiteMapper.selectPage(pageQuery.toPage(), uw);

        List<DeptDailySysUserLite> users = userPage.getRecords();
        if (users == null || users.isEmpty()) {
            Page<AttendanceMonthTableRowVO> empty = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
            empty.setRecords(List.of());
            return empty;
        }

        List<Long> userIds = users.stream().map(DeptDailySysUserLite::getId).toList();
        Map<Long, DeptDailySysUserLite> userById = new HashMap<>(users.size() * 2);
        for (DeptDailySysUserLite u : users) userById.put(u.getId(), u);

        // 月度 sheet（用于 SUBMITTED/DRAFT/NONE）
        Map<Long, DeptAttendanceSheet> sheetByUser = new HashMap<>(users.size() * 2);
        List<DeptAttendanceSheet> sheets = attendanceSheetMapper.selectList(new LambdaQueryWrapper<DeptAttendanceSheet>()
                .eq(DeptAttendanceSheet::getTenantId, tenantId)
                .eq(DeptAttendanceSheet::getYear, year)
                .eq(DeptAttendanceSheet::getMonth, month)
                .in(DeptAttendanceSheet::getUserId, userIds));
        if (sheets != null) {
            for (DeptAttendanceSheet s : sheets) sheetByUser.put(s.getUserId(), s);
        }

        // 明细覆盖项（仅保存非默认项）
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<DeptAttendanceItem> items = attendanceItemMapper.selectList(new LambdaQueryWrapper<DeptAttendanceItem>()
                .eq(DeptAttendanceItem::getTenantId, tenantId)
                .in(DeptAttendanceItem::getUserId, userIds)
                .ge(DeptAttendanceItem::getWorkDate, start)
                .le(DeptAttendanceItem::getWorkDate, end));
        Map<Long, Map<LocalDate, DeptAttendanceItem>> itemMap = new HashMap<>(users.size() * 2);
        if (items != null) {
            for (DeptAttendanceItem it : items) {
                if (it.getUserId() == null || it.getWorkDate() == null) continue;
                itemMap.computeIfAbsent(it.getUserId(), k -> new HashMap<>()).put(it.getWorkDate(), it);
            }
        }

        // 默认日历（是否休息）
        calendarService.ensureYearCached(year);
        Map<LocalDate, com.mdframe.forge.plugin.deptdaily.attendance.entity.DeptCalendarDay> calByDay = calendarService.monthCalendarMap(tenantId, year, month);
        int daysInMonth = ym.lengthOfMonth();

        List<AttendanceMonthTableRowVO> outRows = new ArrayList<>(users.size());
        for (Long uid : userIds) {
            DeptDailySysUserLite u = userById.get(uid);
            DeptAttendanceSheet sh = sheetByUser.get(uid);
            Map<LocalDate, DeptAttendanceItem> overrides = itemMap.get(uid);

            List<String> dayList = new java.util.ArrayList<>(31);
            int work = 0, rest = 0, travel = 0;
            double leave = 0d;

            for (int d = 1; d <= 31; d++) {
                if (d > daysInMonth) {
                    dayList.add("");
                    continue;
                }
                LocalDate date = ym.atDay(d);
                var calRow = calByDay.get(date);
                boolean off = isOffDayForRowLocal(date, calRow);
                String defaultStatus = off ? AttendanceDayStatus.REST : AttendanceDayStatus.WORK;

                String effective = defaultStatus;
                if (overrides != null) {
                    DeptAttendanceItem ov = overrides.get(date);
                    if (ov != null && StringUtils.isNotBlank(ov.getDayStatus())) {
                        effective = ov.getDayStatus();
                    }
                }
                dayList.add(effective);
                if (AttendanceDayStatus.WORK.equals(effective)) work++;
                else if (AttendanceDayStatus.REST.equals(effective)) rest++;
                else if (AttendanceDayStatus.TRAVEL.equals(effective)) travel++;
                else if (AttendanceDayStatus.LEAVE.equals(effective)) {
                    double ld = 1d;
                    if (overrides != null) {
                        DeptAttendanceItem ov = overrides.get(date);
                        if (ov != null && ov.getLeaveDays() != null) ld = ov.getLeaveDays();
                    }
                    leave += ld;
                }
            }

            AttendanceMonthTableRowVO r = new AttendanceMonthTableRowVO();
            r.setUserId(uid);
            r.setUsername(u != null ? u.getUsername() : null);
            r.setRealName(u != null ? u.getRealName() : null);
            r.setEmployeeType(u != null ? u.getEmployeeType() : null);
            r.setSheetStatus(sh != null ? sh.getStatus() : "NONE");
            r.setWorkDays(work);
            r.setRestDays(rest);
            r.setTravelDays(travel);
            r.setLeaveDays(leave);
            r.setDays(dayList);
            outRows.add(r);
        }

        Page<AttendanceMonthTableRowVO> out = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        out.setRecords(outRows);
        return out;
    }

    /**
     * 考勤一览表导出专用：不分页，返回所有人员的当月状态与覆盖项（含 leaveType）。
     * <p>
     * 说明：导出需要将请假类型映射为符号（病/事/休/婚/丧/年/产/探/伤等），因此不能复用仅含 dayStatus 的 VO。
     */
    public List<AttendanceMonthExportUserRow> listAttendanceMonthExportRows(int year, int month,
                                                                            Long deptId, Long officeId,
                                                                            Integer employeeType, String keyword) {
        Long tenantId = tenantOrDefault();

        LambdaQueryWrapper<DeptDailySysUserLite> uw = new LambdaQueryWrapper<DeptDailySysUserLite>()
                .eq(DeptDailySysUserLite::getTenantId, tenantId)
                .eq(DeptDailySysUserLite::getUserStatus, 1)
                .eq(employeeType != null, DeptDailySysUserLite::getEmployeeType, employeeType);

        List<Long> scopeOrgIds = resolveScopeOrgIds(tenantId, deptId, officeId);
        List<Long> scopeUserIds = resolveScopeUserIds(tenantId, scopeOrgIds);
        if (scopeUserIds != null) {
            if (scopeUserIds.isEmpty()) return List.of();
            uw.in(DeptDailySysUserLite::getId, scopeUserIds);
        } else {
            Long scopeOrgId = resolveScopeOrgId(deptId, officeId);
            if (scopeOrgId != null) {
                List<Long> descendant = orgLiteMapper.selectDescendantOrgIds(tenantId, scopeOrgId);
                if (descendant != null && !descendant.isEmpty()) {
                    uw.in(DeptDailySysUserLite::getCreateDept, descendant);
                } else {
                    uw.eq(DeptDailySysUserLite::getCreateDept, scopeOrgId);
                }
            }
        }

        String kw = StringUtils.trimToNull(keyword);
        if (kw != null) {
            uw.and(w -> w.like(DeptDailySysUserLite::getUsername, kw).or().like(DeptDailySysUserLite::getRealName, kw));
        }
        List<DeptDailySysUserLite> users = userLiteMapper.selectList(uw);
        if (users == null || users.isEmpty()) return List.of();

        // 按配置的“姓名顺序”排序（scope=tenant+dept+office+employeeType），未配置则回落按id
        List<String> orderedNames = resolveAttendanceExportOrderNames(deptId, officeId, employeeType);
        if (orderedNames == null || orderedNames.isEmpty()) {
            users = users.stream()
                    .sorted(Comparator.comparingLong(DeptDailySysUserLite::getId))
                    .toList();
        } else {
            Map<String, Integer> idx = new HashMap<>(orderedNames.size() * 2);
            for (int i = 0; i < orderedNames.size(); i++) {
                String n = orderedNames.get(i);
                if (StringUtils.isNotBlank(n) && !idx.containsKey(n)) idx.put(n, i);
            }
            users = users.stream().sorted((a, b) -> {
                String an = StringUtils.trimToEmpty(a.getRealName());
                String bn = StringUtils.trimToEmpty(b.getRealName());
                int ai = idx.getOrDefault(an, Integer.MAX_VALUE);
                int bi = idx.getOrDefault(bn, Integer.MAX_VALUE);
                if (ai != bi) return Integer.compare(ai, bi);
                // 未在名单中的，按姓名、用户名、ID稳定排序
                int c1 = an.compareTo(bn);
                if (c1 != 0) return c1;
                String au = StringUtils.trimToEmpty(a.getUsername());
                String bu = StringUtils.trimToEmpty(b.getUsername());
                int c2 = au.compareTo(bu);
                if (c2 != 0) return c2;
                return Long.compare(a.getId(), b.getId());
            }).toList();
        }

        List<Long> userIds = users.stream().map(DeptDailySysUserLite::getId).toList();

        // sheet 状态：NONE/DRAFT/SUBMITTED（导出文件里一般不展示，但保留以备后续）
        Map<Long, DeptAttendanceSheet> sheetByUser = new HashMap<>(users.size() * 2);
        List<DeptAttendanceSheet> sheets = attendanceSheetMapper.selectList(new LambdaQueryWrapper<DeptAttendanceSheet>()
                .eq(DeptAttendanceSheet::getTenantId, tenantId)
                .eq(DeptAttendanceSheet::getYear, year)
                .eq(DeptAttendanceSheet::getMonth, month)
                .in(DeptAttendanceSheet::getUserId, userIds));
        if (sheets != null) {
            for (DeptAttendanceSheet s : sheets) sheetByUser.put(s.getUserId(), s);
        }

        // 覆盖项（含请假类型）
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<DeptAttendanceItem> items = attendanceItemMapper.selectList(new LambdaQueryWrapper<DeptAttendanceItem>()
                .eq(DeptAttendanceItem::getTenantId, tenantId)
                .in(DeptAttendanceItem::getUserId, userIds)
                .ge(DeptAttendanceItem::getWorkDate, start)
                .le(DeptAttendanceItem::getWorkDate, end));
        Map<Long, Map<LocalDate, DeptAttendanceItem>> itemMap = new HashMap<>(users.size() * 2);
        if (items != null) {
            for (DeptAttendanceItem it : items) {
                if (it.getUserId() == null || it.getWorkDate() == null) continue;
                itemMap.computeIfAbsent(it.getUserId(), k -> new HashMap<>()).put(it.getWorkDate(), it);
            }
        }

        calendarService.ensureYearCached(year);
        Map<LocalDate, com.mdframe.forge.plugin.deptdaily.attendance.entity.DeptCalendarDay> calByDay =
                calendarService.monthCalendarMap(tenantId, year, month);
        int daysInMonth = ym.lengthOfMonth();

        List<AttendanceMonthExportUserRow> out = new ArrayList<>(users.size());
        for (DeptDailySysUserLite u : users) {
            Long uid = u.getId();
            DeptAttendanceSheet sh = sheetByUser.get(uid);
            Map<LocalDate, DeptAttendanceItem> overrides = itemMap.get(uid);

            List<AttendanceDayEffective> effectiveDays = new ArrayList<>(31);
            int work = 0, rest = 0, travel = 0;
            double leave = 0d;

            for (int d = 1; d <= 31; d++) {
                if (d > daysInMonth) {
                    effectiveDays.add(AttendanceDayEffective.empty());
                    continue;
                }
                LocalDate date = ym.atDay(d);
                var calRow = calByDay.get(date);
                boolean off = isOffDayForRowLocal(date, calRow);
                String defaultStatus = off ? AttendanceDayStatus.REST : AttendanceDayStatus.WORK;

                String dayStatus = defaultStatus;
                String leaveType = null;
                if (overrides != null) {
                    DeptAttendanceItem ov = overrides.get(date);
                    if (ov != null && StringUtils.isNotBlank(ov.getDayStatus())) {
                        dayStatus = ov.getDayStatus();
                        leaveType = StringUtils.trimToNull(ov.getLeaveType());
                    }
                }
                effectiveDays.add(new AttendanceDayEffective(dayStatus, leaveType));

                if (AttendanceDayStatus.WORK.equals(dayStatus)) work++;
                else if (AttendanceDayStatus.REST.equals(dayStatus)) rest++;
                else if (AttendanceDayStatus.TRAVEL.equals(dayStatus)) travel++;
                else if (AttendanceDayStatus.LEAVE.equals(dayStatus)) {
                    double ld = 1d;
                    if (overrides != null) {
                        DeptAttendanceItem ov = overrides.get(date);
                        if (ov != null && ov.getLeaveDays() != null) ld = ov.getLeaveDays();
                    }
                    leave += ld;
                }
            }

            AttendanceMonthExportUserRow r = new AttendanceMonthExportUserRow();
            r.setUserId(uid);
            r.setUsername(u.getUsername());
            r.setRealName(u.getRealName());
            r.setEmployeeType(u.getEmployeeType());
            r.setSheetStatus(sh != null ? sh.getStatus() : "NONE");
            r.setWorkDays(work);
            r.setRestDays(rest);
            r.setTravelDays(travel);
            r.setLeaveDays(leave);
            r.setDays(effectiveDays);
            out.add(r);
        }
        return out;
    }

    private List<String> resolveAttendanceExportOrderNames(Long deptId, Long officeId, Integer employeeType) {
        DeptDailyReportSetting setting = getSetting(deptId, officeId, employeeType);
        if (setting == null) return List.of();
        String raw = StringUtils.trimToNull(setting.getAttendanceExportOrder());
        if (raw == null) return List.of();
        // 支持：换行/逗号/顿号分隔
        String normalized = raw.replace("，", ",").replace("、", ",");
        String[] parts = normalized.split("[,\\r\\n]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String n = StringUtils.trimToNull(p);
            if (n != null) out.add(n);
        }
        return out;
    }

    @lombok.Data
    public static class AttendanceMonthExportUserRow {
        private Long userId;
        private String username;
        private String realName;
        private Integer employeeType;
        private String sheetStatus;
        private Integer workDays;
        private Integer restDays;
        private Integer travelDays;
        private Double leaveDays;
        /**
         * 1..31：当日有效状态（含请假类型）
         */
        private List<AttendanceDayEffective> days;
    }

    public record AttendanceDayEffective(String dayStatus, String leaveType) {
        public static AttendanceDayEffective empty() {
            return new AttendanceDayEffective("", null);
        }
    }

    /**
     * 复刻 {@code DeptCalendarService#isOffDayForRow} 逻辑（原方法为包可见，这里无法直接调用）。
     */
    private static boolean isOffDayForRowLocal(LocalDate date, com.mdframe.forge.plugin.deptdaily.attendance.entity.DeptCalendarDay row) {
        if (date == null) return false;
        boolean weekend = date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
        boolean base = weekend; // 平日基线=上班(false)，周末基线=休(true)
        if (row == null || row.getIsOffDay() == null) return base;
        if (weekend) {
            // 兼容历史 WEEKENDS 噪声行（仅名称为“周六/周日”的误导条目）
            String name = row.getName();
            if ("WEEKENDS".equals(row.getSource()) && ("周六".equals(name) || "周日".equals(name))) {
                return true;
            }
            return row.getIsOffDay() == 1;
        }
        return row.getIsOffDay() == 1;
    }

    /**
     * 按起始年月批量刷新 fill_state（面向统览/未填报列表），避免查询时做大规模补齐计算。
     * <p>
     * module: ATTENDANCE / WORK_REPORT
     *
     * @return upsert 行数（近似）
     */
    public int refreshFillState(String module, Long deptId, Long officeId, Integer employeeType, String startYm, String endYm) {
        if (StringUtils.isBlank(module)) throw new IllegalArgumentException("module不能为空");
        if (StringUtils.isBlank(startYm)) throw new IllegalArgumentException("startYm不能为空");
        Long tenantId = tenantOrDefault();

        YearMonth start = YearMonth.parse(startYm, YM_FMT);
        YearMonth end = (StringUtils.isBlank(endYm) ? YearMonth.now() : YearMonth.parse(endYm, YM_FMT));
        YearMonth now = YearMonth.now();
        if (end.isAfter(now)) end = now;
        if (start.isAfter(end)) return 0;

        // 组织范围：优先 sys_user_org（多组织），包含所有下属部门；管理员默认全租户
        List<Long> scopeOrgIds = resolveScopeOrgIds(tenantId, deptId, officeId);
        List<Long> scopeUserIds = resolveScopeUserIds(tenantId, scopeOrgIds);

        // 用户集合：按 tenant + employeeType + org范围
        LambdaQueryWrapper<DeptDailySysUserLite> uw = new LambdaQueryWrapper<DeptDailySysUserLite>()
                .eq(DeptDailySysUserLite::getTenantId, tenantId)
                .eq(employeeType != null, DeptDailySysUserLite::getEmployeeType, employeeType)
                .eq(DeptDailySysUserLite::getUserStatus, 1);
        if (scopeUserIds != null) {
            if (scopeUserIds.isEmpty()) return 0;
            uw.in(DeptDailySysUserLite::getId, scopeUserIds);
        } else {
            // 兼容：回落 create_dept
            Long scopeOrgId = resolveScopeOrgId(deptId, officeId);
            if (scopeOrgId != null) {
                List<Long> descendant = orgLiteMapper.selectDescendantOrgIds(tenantId, scopeOrgId);
                if (descendant != null && !descendant.isEmpty()) {
                    uw.in(DeptDailySysUserLite::getCreateDept, descendant);
                } else {
                    uw.eq(DeptDailySysUserLite::getCreateDept, scopeOrgId);
                }
            }
        }
        List<DeptDailySysUserLite> users = userLiteMapper.selectList(uw);
        if (users == null || users.isEmpty()) return 0;

        List<Long> userIds = users.stream().map(DeptDailySysUserLite::getId).toList();
        Map<Long, DeptDailySysUserLite> userById = new HashMap<>(users.size() * 2);
        for (DeptDailySysUserLite u : users) userById.put(u.getId(), u);

        int total = 0;
        YearMonth cur = start;
        while (!cur.isAfter(end)) {
            String ym = cur.format(YM_FMT);
            LocalDateTime ts = LocalDateTime.now();

            Map<Long, String> statusByUser = new HashMap<>(userIds.size() * 2);
            if ("ATTENDANCE".equalsIgnoreCase(module)) {
                List<DeptAttendanceSheet> sheets = attendanceSheetMapper.selectList(new LambdaQueryWrapper<DeptAttendanceSheet>()
                        .eq(DeptAttendanceSheet::getTenantId, tenantId)
                        .eq(DeptAttendanceSheet::getYear, cur.getYear())
                        .eq(DeptAttendanceSheet::getMonth, cur.getMonthValue())
                        .in(DeptAttendanceSheet::getUserId, userIds));
                if (sheets != null) {
                    for (DeptAttendanceSheet s : sheets) {
                        statusByUser.put(s.getUserId(), s.getStatus());
                    }
                }
            } else if ("WORK_REPORT".equalsIgnoreCase(module)) {
                // 个人月报总表：DRAFT/SUBMITTED
                List<com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailyUserMonthReportSheet> sheets =
                        userSheetMapper.selectList(new LambdaQueryWrapper<com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailyUserMonthReportSheet>()
                                .eq(com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailyUserMonthReportSheet::getTenantId, tenantId)
                                .eq(com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailyUserMonthReportSheet::getReportYm, ym)
                                .in(com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailyUserMonthReportSheet::getUserId, userIds));
                if (sheets != null) {
                    for (var s : sheets) {
                        statusByUser.put(s.getUserId(), s.getStatus());
                    }
                }
            } else {
                throw new IllegalArgumentException("暂不支持的module: " + module);
            }

            // 组装 upsert 批次
            List<DeptDailyFillState> batch = new ArrayList<>(userIds.size());
            for (Long uid : userIds) {
                DeptDailySysUserLite u = userById.get(uid);
                DeptDailyFillState st = new DeptDailyFillState();
                st.setTenantId(tenantId);
                // 仅用于列表筛选的 scope 标识：指定dept/office时用其值；否则用当前用户主组织
                st.setDeptId(resolveScopeOrgId(deptId, officeId));
                st.setOfficeId(null);
                st.setEmployeeType(u != null ? u.getEmployeeType() : null);
                st.setModule(module.toUpperCase());
                st.setYear(cur.getYear());
                st.setMonth(cur.getMonthValue());
                st.setYm(ym);
                st.setUserId(uid);
                st.setStatus(statusByUser.getOrDefault(uid, "NONE"));
                st.setLastCalcTime(ts);
                st.setUpdateTime(ts);
                st.setUpdateBy(SessionHelper.getUserId());
                batch.add(st);
            }

            // 分批 upsert，避免超大 SQL
            final int chunkSize = 500;
            for (int i = 0; i < batch.size(); i += chunkSize) {
                int j = Math.min(i + chunkSize, batch.size());
                total += fillStateMapper.upsertBatch(batch.subList(i, j));
            }

            cur = cur.plusMonths(1);
        }
        return total;
    }

    private static Long tenantOrDefault() {
        Long tid = SessionHelper.getTenantId();
        return tid != null ? tid : 1L;
    }

    /**
     * 统览范围组织集合：
     * - 指定 officeId/deptId：取该组织及其子孙
     * - 未指定：管理员/租户管理员=全租户（返回 null 表示不做组织过滤）；普通用户=取当前用户所有组织及其子孙
     */
    private List<Long> resolveScopeOrgIds(Long tenantId, Long deptId, Long officeId) {
        if (deptId == null && officeId == null && (SessionHelper.isAdmin() || SessionHelper.isTenantAdmin())) {
            return null; // 全租户
        }

        Set<Long> base = new LinkedHashSet<>();
        if (officeId != null) base.add(officeId);
        else if (deptId != null) base.add(deptId);
        else {
            List<Long> orgIds = SessionHelper.getOrgIds();
            if (orgIds != null) base.addAll(orgIds);
            Long main = SessionHelper.getMainOrgId();
            if (main != null) base.add(main);
        }

        if (base.isEmpty()) {
            // 兜底：无组织信息时不做过滤，避免出现“全空”
            return null;
        }

        Set<Long> out = new LinkedHashSet<>();
        for (Long orgId : base) {
            if (orgId == null) continue;
            List<Long> d = orgLiteMapper.selectDescendantOrgIds(tenantId, orgId);
            if (d != null && !d.isEmpty()) out.addAll(d);
            else out.add(orgId);
        }
        return new ArrayList<>(out);
    }

    /**
     * 根据组织范围取用户ID；当 scopeOrgIds 为 null 时返回 null（表示不限制用户范围）。
     * 如果 sys_user_org 没有数据或查询异常，则返回 null 让调用方回落到 create_dept 方案。
     */
    private List<Long> resolveScopeUserIds(Long tenantId, List<Long> scopeOrgIds) {
        if (scopeOrgIds == null) return null;
        if (scopeOrgIds.isEmpty()) return List.of();
        try {
            List<Long> ids = userOrgLiteMapper.selectDistinctUserIdsByOrgIds(tenantId, scopeOrgIds);
            if (ids == null) return List.of();
            return ids;
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 统览范围：优先 officeId，其次 deptId；都为空则取当前登录用户主组织（本部门）。
     */
    private static Long resolveScopeOrgId(Long deptId, Long officeId) {
        if (officeId != null) return officeId;
        if (deptId != null) return deptId;
        return SessionHelper.getMainOrgId();
    }
}

