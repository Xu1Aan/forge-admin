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
     * 确保某年日历已缓存到库（无则全量拉取并写入）。
     * <p>
     * 已有该年行时<b>不会</b>再请求全量接口（避免每次打开月视图都调第三方）；
     * 需与 jiejiari 保持完全一致时请调用 {@link #refreshYear(int)}（例如通过 REST「同步节假日」），
     * 或删除库中该年数据后让本方法再次全量拉取。
     */
    @Transactional(rollbackFor = Exception.class)
    public void ensureYearCached(int year) {
        Long tenantId = tenantOrDefault();
        Long cnt = calendarDayMapper.selectCount(new LambdaQueryWrapper<DeptCalendarDay>()
                .eq(DeptCalendarDay::getTenantId, tenantId)
                .eq(DeptCalendarDay::getYear, year));
        if (cnt != null && cnt > 0) {
            Long wd = calendarDayMapper.selectCount(new LambdaQueryWrapper<DeptCalendarDay>()
                    .eq(DeptCalendarDay::getTenantId, tenantId)
                    .eq(DeptCalendarDay::getYear, year)
                    .eq(DeptCalendarDay::getSource, "WORKDAYS"));
            if (wd == null || wd == 0) {
                backfillWorkdays(year);
            }
            return;
        }
        refreshYear(year);
    }

    private void backfillWorkdays(int year) {
        Long tenantId = tenantOrDefault();
        for (JieJiaRiApiClient.DayInfo d : apiClient.fetchWorkdays(year).values()) {
            if (d == null || StringUtils.isBlank(d.getDate()) || d.getIsOffDay() == null) {
                continue;
            }
            upsert(tenantId, year, d, "WORKDAYS");
        }
    }

    /**
     * 强制刷新某年缓存（覆盖写）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshYear(int year) {
        Long tenantId = tenantOrDefault();

        Map<String, JieJiaRiApiClient.DayInfo> holidays = apiClient.fetchHolidays(year);
        Map<String, JieJiaRiApiClient.DayInfo> weekends = apiClient.fetchWeekends(year);
        Map<String, JieJiaRiApiClient.DayInfo> workdays = apiClient.fetchWorkdays(year);

        // holidays 先写入（法定、调休、补班以「节假日」结果为准，含周末补班 isOffDay=false 等）
        for (JieJiaRiApiClient.DayInfo d : holidays.values()) {
            upsert(tenantId, year, d, "HOLIDAYS");
        }
        /**
         * jiejiari {@code /v1/weekends} 会列出全年每个周六/周日，且对「周六/周日」普遍返回 {@code isOffDay: false}，
         * 与「是否国家休息日」的直觉相反；若与 holidays 同序写入会<strong>覆盖</strong>清明/国庆等周末法定假。
         * 对仅标注「周六」「周日」的条目不写入；其余（极少见）不覆盖已存在的 HOLIDAYS 行。
         */
        for (JieJiaRiApiClient.DayInfo d : weekends.values()) {
            if (d == null || StringUtils.isBlank(d.getDate()) || d.getIsOffDay() == null) {
                continue;
            }
            if (isJieJiaGenericWeekendLabel(d.getName())) {
                continue;
            }
            LocalDate day = LocalDate.parse(d.getDate(), DF);
            DeptCalendarDay existing = calendarDayMapper.selectOne(new LambdaQueryWrapper<DeptCalendarDay>()
                    .eq(DeptCalendarDay::getTenantId, tenantId)
                    .eq(DeptCalendarDay::getDay, day)
                    .last("limit 1"));
            if (existing != null && "HOLIDAYS".equals(existing.getSource())) {
                continue;
            }
            upsert(tenantId, year, d, "WEEKENDS");
        }
        for (JieJiaRiApiClient.DayInfo d : workdays.values()) {
            if (d == null || StringUtils.isBlank(d.getDate()) || d.getIsOffDay() == null) {
                continue;
            }
            upsert(tenantId, year, d, "WORKDAYS");
        }
    }

    /**
     * 计算某天是否休息（国家日历 + 自然周末默认）。
     * <ul>
     *   <li><b>周一～周五</b>：无库行则默认上班；有行则以 {@code is_off_day=1} 为休、{@code 0} 为上班。</li>
     *   <li><b>周六、周日</b>：无库行则默认<b>休</b>。有行时，以「节假日」接口写入的 {@code is_off_day} 为准（含周末补班日 {@code 0}）。
     *   已忽略 jiejiari「周末」列表中仅名称为「周六/周日」的误导行；若历史库中仍有此类 WEEKEnds 行，读时同规则忽略。</li>
     * </ul>
     * 数据来源为拉取后写入的 {@code dept_calendar_day}（见 {@link #refreshYear(int)}）。
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
            if (isJieJiaWeekendListNoiseRow(row)) {
                return true;
            }
            return row.getIsOffDay() == 1;
        }
        return row.getIsOffDay() == 1;
    }

    /**
     * 自然周六、日（公历）。
     */
    public boolean isNaturalWeekend(LocalDate date) {
        return date != null && isWeekend(date);
    }

    /**
     * 调休补班：自然周末且国家日历标记为上班（is_off_day=0）。
     */
    public boolean isCompensatoryWorkday(LocalDate date, DeptCalendarDay row) {
        if (date == null || row == null || row.getIsOffDay() == null) {
            return false;
        }
        if (!isWeekend(date) || row.getIsOffDay() != 0) {
            return false;
        }
        if (isJieJiaWeekendListNoiseRow(row)) {
            return false;
        }
        return true;
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
        if (date == null || row == null) return null;
        if (isJieJiaWeekendListNoiseRow(row)) {
            return null;
        }
        return StringUtils.trimToNull(row.getName());
    }

    /**
     * 读取某日国家日历缓存行（无则 null）。
     */
    public DeptCalendarDay getCalendarRow(long tenantId, LocalDate day) {
        if (day == null) {
            return null;
        }
        return calendarDayMapper.selectOne(new LambdaQueryWrapper<DeptCalendarDay>()
                .eq(DeptCalendarDay::getTenantId, tenantId)
                .eq(DeptCalendarDay::getDay, day)
                .last("limit 1"));
    }

    private DeptCalendarDay getRowOrNull(long tenantId, LocalDate day) {
        return getCalendarRow(tenantId, day);
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
     * jiejiari「周末」接口对普通周六/日固定返回 {@code isOffDay:false}＋「周六/周日」文案，不表示调休补班，不得参与判定。
     */
    private static boolean isJieJiaWeekendListNoiseRow(DeptCalendarDay row) {
        return row != null
                && "WEEKENDS".equals(row.getSource())
                && isJieJiaGenericWeekendLabel(row.getName());
    }

    private static boolean isJieJiaGenericWeekendLabel(String name) {
        if (name == null) {
            return false;
        }
        String t = name.trim();
        return "周六".equals(t) || "周日".equals(t) || "星期六".equals(t) || "星期日".equals(t);
    }

    private static Long tenantOrDefault() {
        Long tid = SessionHelper.getTenantId();
        return tid != null ? tid : 1L;
    }
}

