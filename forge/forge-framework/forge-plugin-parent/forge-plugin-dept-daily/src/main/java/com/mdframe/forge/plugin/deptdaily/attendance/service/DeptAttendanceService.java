package com.mdframe.forge.plugin.deptdaily.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.deptdaily.attendance.dto.AttendanceDayDTO;
import com.mdframe.forge.plugin.deptdaily.attendance.dto.AttendanceMonthViewDTO;
import com.mdframe.forge.plugin.deptdaily.attendance.dto.AttendanceToggleDayRequest;
import com.mdframe.forge.plugin.deptdaily.attendance.dto.AttendanceUpdateDayRequest;
import com.mdframe.forge.plugin.deptdaily.attendance.entity.DeptAttendanceItem;
import com.mdframe.forge.plugin.deptdaily.attendance.entity.DeptAttendanceSheet;
import com.mdframe.forge.plugin.deptdaily.attendance.entity.DeptCalendarDay;
import com.mdframe.forge.plugin.deptdaily.attendance.enums.AttendanceDayStatus;
import com.mdframe.forge.plugin.deptdaily.attendance.mapper.DeptAttendanceItemMapper;
import com.mdframe.forge.plugin.deptdaily.attendance.mapper.DeptAttendanceSheetMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeptAttendanceService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DeptAttendanceSheetMapper sheetMapper;
    private final DeptAttendanceItemMapper itemMapper;
    private final DeptCalendarService calendarService;

    /**
     * 一键填报整月：仅确保月度单存在（DRAFT），不强制落库默认明细；明细仅保存覆盖项。
     */
    @Transactional(rollbackFor = Exception.class)
    public DeptAttendanceSheet oneClickFill(int year, int month) {
        Long tenantId = tenantOrDefault();
        Long userId = requireUserId();

        DeptAttendanceSheet sheet = sheetMapper.selectOne(new LambdaQueryWrapper<DeptAttendanceSheet>()
                .eq(DeptAttendanceSheet::getTenantId, tenantId)
                .eq(DeptAttendanceSheet::getUserId, userId)
                .eq(DeptAttendanceSheet::getYear, year)
                .eq(DeptAttendanceSheet::getMonth, month)
                .last("limit 1"));
        if (sheet != null) {
            return sheet;
        }

        DeptAttendanceSheet s = new DeptAttendanceSheet();
        s.setTenantId(tenantId);
        s.setUserId(userId);
        s.setYear(year);
        s.setMonth(month);
        s.setStatus("DRAFT");
        sheetMapper.insert(s);
        return s;
    }

    public AttendanceMonthViewDTO getMonthView(int year, int month) {
        Long tenantId = tenantOrDefault();
        Long userId = requireUserId();

        DeptAttendanceSheet sheet = sheetMapper.selectOne(new LambdaQueryWrapper<DeptAttendanceSheet>()
                .eq(DeptAttendanceSheet::getTenantId, tenantId)
                .eq(DeptAttendanceSheet::getUserId, userId)
                .eq(DeptAttendanceSheet::getYear, year)
                .eq(DeptAttendanceSheet::getMonth, month)
                .last("limit 1"));

        Long sheetId = sheet != null ? sheet.getId() : null;
        Map<LocalDate, DeptAttendanceItem> itemByDate = new HashMap<>();
        if (sheetId != null) {
            List<DeptAttendanceItem> items = itemMapper.selectList(new LambdaQueryWrapper<DeptAttendanceItem>()
                    .eq(DeptAttendanceItem::getTenantId, tenantId)
                    .eq(DeptAttendanceItem::getSheetId, sheetId));
            for (DeptAttendanceItem it : items) {
                if (it.getWorkDate() != null) itemByDate.put(it.getWorkDate(), it);
            }
        }

        YearMonth ym = YearMonth.of(year, month);
        int daysInMonth = ym.lengthOfMonth();

        // 年维度只拉取/检查一次第三方假日缓存，避免 28~31 天各自触发 ensure（大量 COUNT/潜在重复 HTTP）
        calendarService.ensureYearCached(year);
        // 当月日历行一次查出，按天用 Map 命中，避免对 dept_calendar_day 做 50+ 次点查
        Map<LocalDate, DeptCalendarDay> calByDay = calendarService.monthCalendarMap(tenantId, year, month);

        List<AttendanceDayDTO> days = new java.util.ArrayList<>(daysInMonth);
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate date = ym.atDay(d);
            DeptCalendarDay calRow = calByDay.get(date);
            boolean off = calendarService.isOffDayForRow(date, calRow);
            String defaultStatus = off ? AttendanceDayStatus.REST : AttendanceDayStatus.WORK;
            String name = calendarService.dayNameForRow(date, calRow);

            DeptAttendanceItem override = itemByDate.get(date);

            AttendanceDayDTO dto = new AttendanceDayDTO();
            dto.setDate(date.format(DF));
            dto.setName(name);
            dto.setOffDay(off);
            dto.setDefaultStatus(defaultStatus);

            if (override != null) {
                dto.setStatus(override.getDayStatus());
                dto.setLeaveType(override.getLeaveType());
                dto.setRemark(override.getRemark());
            } else {
                dto.setStatus(defaultStatus);
            }
            days.add(dto);
        }

        AttendanceMonthViewDTO out = new AttendanceMonthViewDTO();
        out.setSheetId(sheetId);
        out.setYear(year);
        out.setMonth(month);
        out.setStatus(sheet != null ? sheet.getStatus() : "NONE");
        out.setDays(days);
        return out;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDay(int year, int month, AttendanceUpdateDayRequest req) {
        if (req == null || StringUtils.isBlank(req.getDate())) {
            throw new IllegalArgumentException("date不能为空");
        }
        Long tenantId = tenantOrDefault();
        Long userId = requireUserId();

        DeptAttendanceSheet sheet = oneClickFill(year, month);
        if (!"DRAFT".equals(sheet.getStatus())) {
            throw new IllegalStateException("该月已提交，无法修改");
        }

        LocalDate date = LocalDate.parse(req.getDate(), DF);
        if (date.getYear() != year || date.getMonthValue() != month) {
            throw new IllegalArgumentException("date不在指定年月内");
        }

        String status = StringUtils.trimToEmpty(req.getStatus());
        if (StringUtils.isBlank(status)) {
            throw new IllegalArgumentException("status不能为空");
        }
        if (AttendanceDayStatus.LEAVE.equals(status) && StringUtils.isBlank(req.getLeaveType())) {
            throw new IllegalArgumentException("请假类型不能为空");
        }

        boolean off = calendarService.isOffDay(date);
        String defaultStatus = off ? AttendanceDayStatus.REST : AttendanceDayStatus.WORK;

        DeptAttendanceItem existing = itemMapper.selectOne(new LambdaQueryWrapper<DeptAttendanceItem>()
                .eq(DeptAttendanceItem::getTenantId, tenantId)
                .eq(DeptAttendanceItem::getSheetId, sheet.getId())
                .eq(DeptAttendanceItem::getWorkDate, date)
                .last("limit 1"));

        boolean isDefaultAndNoExtra = status.equals(defaultStatus)
                && StringUtils.isBlank(req.getLeaveType())
                && StringUtils.isBlank(req.getRemark());

        if (isDefaultAndNoExtra) {
            if (existing != null) {
                itemMapper.deleteById(existing.getId());
            }
            return;
        }

        DeptAttendanceItem it = existing != null ? existing : new DeptAttendanceItem();
        it.setTenantId(tenantId);
        it.setSheetId(sheet.getId());
        it.setUserId(userId);
        it.setWorkDate(date);
        it.setDayStatus(status);
        it.setLeaveType(StringUtils.trimToNull(req.getLeaveType()));
        it.setRemark(StringUtils.trimToNull(req.getRemark()));

        if (existing == null) {
            itemMapper.insert(it);
        } else {
            itemMapper.updateById(it);
        }
    }

    /**
     * 点击切换（日历格）：默认(出勤/休息) -> 出差 -> 请假 -> 回到默认。
     * <p>
     * 前端只需提交 date；当切换到请假时，需要提交 leaveType。
     */
    @Transactional(rollbackFor = Exception.class)
    public AttendanceDayDTO toggleDay(int year, int month, AttendanceToggleDayRequest req) {
        if (req == null || StringUtils.isBlank(req.getDate())) {
            throw new IllegalArgumentException("date不能为空");
        }
        Long tenantId = tenantOrDefault();
        Long userId = requireUserId();

        DeptAttendanceSheet sheet = oneClickFill(year, month);
        if (!"DRAFT".equals(sheet.getStatus())) {
            throw new IllegalStateException("该月已提交，无法修改");
        }

        LocalDate date = LocalDate.parse(req.getDate(), DF);
        if (date.getYear() != year || date.getMonthValue() != month) {
            throw new IllegalArgumentException("date不在指定年月内");
        }

        boolean off = calendarService.isOffDay(date);
        String defaultStatus = off ? AttendanceDayStatus.REST : AttendanceDayStatus.WORK;
        String dayName = calendarService.getDayName(date);

        DeptAttendanceItem existing = itemMapper.selectOne(new LambdaQueryWrapper<DeptAttendanceItem>()
                .eq(DeptAttendanceItem::getTenantId, tenantId)
                .eq(DeptAttendanceItem::getSheetId, sheet.getId())
                .eq(DeptAttendanceItem::getWorkDate, date)
                .last("limit 1"));

        String currentEffective = existing != null ? existing.getDayStatus() : defaultStatus;
        String next;
        if (AttendanceDayStatus.TRAVEL.equals(currentEffective)) {
            next = AttendanceDayStatus.LEAVE;
        } else if (AttendanceDayStatus.LEAVE.equals(currentEffective)) {
            next = defaultStatus;
        } else {
            // default(WORK/REST) 或其它未知状态 -> TRAVEL
            next = AttendanceDayStatus.TRAVEL;
        }

        String leaveType = StringUtils.trimToNull(req.getLeaveType());
        String remark = StringUtils.trimToNull(req.getRemark());

        if (AttendanceDayStatus.LEAVE.equals(next) && StringUtils.isBlank(leaveType)) {
            throw new IllegalArgumentException("请假类型不能为空");
        }

        boolean isDefaultAndNoExtra = next.equals(defaultStatus)
                && StringUtils.isBlank(leaveType)
                && StringUtils.isBlank(remark);

        if (isDefaultAndNoExtra) {
            if (existing != null) {
                itemMapper.deleteById(existing.getId());
            }
        } else {
            DeptAttendanceItem it = existing != null ? existing : new DeptAttendanceItem();
            it.setTenantId(tenantId);
            it.setSheetId(sheet.getId());
            it.setUserId(userId);
            it.setWorkDate(date);
            it.setDayStatus(next);
            it.setLeaveType(AttendanceDayStatus.LEAVE.equals(next) ? leaveType : null);
            it.setRemark(remark);
            if (existing == null) {
                itemMapper.insert(it);
            } else {
                itemMapper.updateById(it);
            }
        }

        AttendanceDayDTO dto = new AttendanceDayDTO();
        dto.setDate(date.format(DF));
        dto.setName(dayName);
        dto.setOffDay(off);
        dto.setDefaultStatus(defaultStatus);
        dto.setStatus(next);
        dto.setLeaveType(AttendanceDayStatus.LEAVE.equals(next) ? leaveType : null);
        dto.setRemark(remark);
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public void submit(int year, int month) {
        DeptAttendanceSheet sheet = oneClickFill(year, month);
        if ("SUBMITTED".equals(sheet.getStatus())) {
            return;
        }
        sheet.setStatus("SUBMITTED");
        sheet.setSubmittedAt(java.time.LocalDateTime.now());
        sheetMapper.updateById(sheet);
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

