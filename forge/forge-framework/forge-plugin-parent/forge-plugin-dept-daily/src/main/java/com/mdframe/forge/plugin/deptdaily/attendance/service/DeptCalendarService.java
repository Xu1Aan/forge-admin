package com.mdframe.forge.plugin.deptdaily.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.deptdaily.attendance.entity.DeptCalendarDay;
import com.mdframe.forge.plugin.deptdaily.attendance.mapper.DeptCalendarDayMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeptCalendarService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DeptCalendarDayMapper calendarDayMapper;
    private final JieJiaRiApiClient apiClient;

    /**
     * 确保某年日历已缓存到库（无则拉取并写入）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void ensureYearCached(int year) {
        Long tenantId = tenantOrDefault();
        Long cnt = calendarDayMapper.selectCount(new LambdaQueryWrapper<DeptCalendarDay>()
                .eq(DeptCalendarDay::getTenantId, tenantId)
                .eq(DeptCalendarDay::getYear, year));
        if (cnt != null && cnt > 0) {
            return;
        }
        refreshYear(year);
    }

    /**
     * 强制刷新某年缓存（覆盖写）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshYear(int year) {
        Long tenantId = tenantOrDefault();

        Map<String, JieJiaRiApiClient.DayInfo> holidays = apiClient.fetchHolidays(year);
        Map<String, JieJiaRiApiClient.DayInfo> weekends = apiClient.fetchWeekends(year);

        // holidays 先写入
        for (JieJiaRiApiClient.DayInfo d : holidays.values()) {
            upsert(tenantId, year, d, "HOLIDAYS");
        }
        // weekends 再写入（调休上班日/周末信息），若同日已存在则覆盖（以接口为准）
        for (JieJiaRiApiClient.DayInfo d : weekends.values()) {
            upsert(tenantId, year, d, "WEEKENDS");
        }
    }

    /**
     * 计算某天是否休息（含调休/法定节假日覆盖）。
     * 规则：平日默认需出勤；自然周六日默认休。缓存行 isOff 覆盖「是否休」。
     * <p>
     * 对<strong>自然周六/周日</strong>：仅当国家安排需上班的「补班」等才按上班（isOff=0 且名称可辨认为补班/调休上班）；
     * 避免第三方将普通周末误标为「上班」导致主界面显示为出勤。平日逻辑不变。
     */
    public boolean isOffDay(LocalDate date) {
        if (date == null) return false;
        ensureYearCached(date.getYear());
        return isOffDayAfterCacheReady(date);
    }

    /**
     * 一次查出当月内所有假日行，月视图里按天从 Map 取，避免 28~31×2 次点查。
     */
    public Map<LocalDate, DeptCalendarDay> monthCalendarMap(long tenantId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<DeptCalendarDay> list = calendarDayMapper.selectList(new LambdaQueryWrapper<DeptCalendarDay>()
                .eq(DeptCalendarDay::getTenantId, tenantId)
                .eq(DeptCalendarDay::getYear, year)
                .ge(DeptCalendarDay::getDay, start)
                .le(DeptCalendarDay::getDay, end));
        if (list == null || list.isEmpty()) {
            return Map.of();
        }
        Map<LocalDate, DeptCalendarDay> m = new HashMap<>((int) (list.size() / 0.75f) + 1);
        for (DeptCalendarDay d : list) {
            if (d.getDay() != null) {
                m.put(d.getDay(), d);
            }
        }
        return m;
    }

    /**
     * 对指定年已执行过 {@link #ensureYearCached(int)} 后，可在同月循环内复用，避免对每一天都做一次「是否已缓存」的 DB 检查。
     */
    boolean isOffDayAfterCacheReady(LocalDate date) {
        if (date == null) return false;
        return isOffDayForRow(date, getRowOrNull(tenantOrDefault(), date));
    }

    boolean isOffDayForRow(LocalDate date, DeptCalendarDay row) {
        if (date == null) return false;
        boolean weekend = isWeekend(date);
        // 平日基线=上班(false)；周末基线=休(true)
        boolean base = weekend;
        if (row == null || row.getIsOffDay() == null) {
            return base;
        }
        if (weekend) {
            if (row.getIsOffDay() == 1) {
                return true; // 明确休息（如落在周末的法定假等）
            }
            if (row.getIsOffDay() == 0) {
                return !isCompensatoryWorkdayLabel(row.getName());
            }
        }
        return row.getIsOffDay() == 1;
    }

    public String getDayName(LocalDate date) {
        if (date == null) return null;
        ensureYearCached(date.getYear());
        return getDayNameAfterCacheReady(date);
    }

    /**
     * 对 {@link #ensureYearCached(int)} 所覆盖的年，与 {@link #isOffDayAfterCacheReady} 成对用于月视图批量渲染。
     */
    String getDayNameAfterCacheReady(LocalDate date) {
        if (date == null) return null;
        return dayNameForRow(date, getRowOrNull(tenantOrDefault(), date));
    }

    String dayNameForRow(LocalDate date, DeptCalendarDay row) {
        if (date == null) return null;
        if (row == null) return null;
        if (isWeekend(date) && row.getIsOffDay() != null
                && row.getIsOffDay() == 0
                && !isCompensatoryWorkdayLabel(row.getName())) {
            return null;
        }
        return StringUtils.trimToNull(row.getName());
    }

    private DeptCalendarDay getRowOrNull(long tenantId, LocalDate day) {
        if (day == null) return null;
        return calendarDayMapper.selectOne(new LambdaQueryWrapper<DeptCalendarDay>()
                .eq(DeptCalendarDay::getTenantId, tenantId)
                .eq(DeptCalendarDay::getDay, day)
                .last("limit 1"));
    }

    private void upsert(Long tenantId, int year, JieJiaRiApiClient.DayInfo info, String source) {
        if (info == null || StringUtils.isBlank(info.getDate()) || info.getIsOffDay() == null) {
            return;
        }
        LocalDate day = LocalDate.parse(info.getDate(), DF);
        DeptCalendarDay existing = calendarDayMapper.selectOne(new LambdaQueryWrapper<DeptCalendarDay>()
                .eq(DeptCalendarDay::getTenantId, tenantId)
                .eq(DeptCalendarDay::getDay, day)
                .last("limit 1"));

        DeptCalendarDay d = existing != null ? existing : new DeptCalendarDay();
        d.setTenantId(tenantId);
        d.setYear(year);
        d.setDay(day);
        d.setName(StringUtils.trimToNull(info.getName()));
        d.setIsOffDay(Boolean.TRUE.equals(info.getIsOffDay()) ? 1 : 0);
        d.setSource(source);

        if (existing == null) {
            calendarDayMapper.insert(d);
        } else {
            calendarDayMapper.updateById(d);
        }
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek w = date.getDayOfWeek();
        return w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY;
    }

    /**
     * 国家「补班」等需要在周末/假日调为上班日：通过名称与常见接口文案识别。
     * 无名称、仅误标 isOff=0 的周末按 {@link #isOffDayAfterCacheReady} 仍计为休息。
     */
    private static boolean isCompensatoryWorkdayLabel(String name) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        String n = name;
        if (n.contains("补班")) {
            return true;
        }
        if (n.contains("调休") && n.contains("班")) {
            return true;
        }
        // 部分接口对补班日仅返回「班」
        return "班".equals(n.trim());
    }

    private static Long tenantOrDefault() {
        Long tid = SessionHelper.getTenantId();
        return tid != null ? tid : 1L;
    }
}

