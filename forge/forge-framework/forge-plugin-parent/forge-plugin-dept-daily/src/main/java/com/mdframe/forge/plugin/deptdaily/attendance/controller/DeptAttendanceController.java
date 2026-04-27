package com.mdframe.forge.plugin.deptdaily.attendance.controller;

import com.mdframe.forge.plugin.deptdaily.attendance.dto.AttendanceMonthViewDTO;
import com.mdframe.forge.plugin.deptdaily.attendance.dto.AttendanceDayDTO;
import com.mdframe.forge.plugin.deptdaily.attendance.dto.AttendanceToggleDayRequest;
import com.mdframe.forge.plugin.deptdaily.attendance.dto.AttendanceUpdateDayRequest;
import com.mdframe.forge.plugin.deptdaily.attendance.service.DeptAttendanceService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/dept-daily/attendance", "/api/dept-daily/attendance"})
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class DeptAttendanceController {

    private final DeptAttendanceService attendanceService;

    /**
     * 一键填报整月：创建月度填报单（DRAFT）
     */
    @PostMapping("/sheet/one-click-fill")
    public RespInfo<Long> oneClickFill(@RequestParam int year, @RequestParam int month) {
        return RespInfo.success(attendanceService.oneClickFill(year, month).getId());
    }

    /**
     * 获取月视图（默认：工作日出勤/周末休息；节假日/调休以第三方接口缓存覆盖）
     */
    @GetMapping("/sheet/view")
    public RespInfo<AttendanceMonthViewDTO> view(@RequestParam int year, @RequestParam int month) {
        return RespInfo.success(attendanceService.getMonthView(year, month));
    }

    /**
     * 更新某天（TRAVEL/LEAVE等）；当回到默认状态且无附加信息时，会自动删除覆盖项
     */
    @PutMapping("/day")
    public RespInfo<Void> updateDay(@RequestParam int year, @RequestParam int month, @RequestBody AttendanceUpdateDayRequest req) {
        attendanceService.updateDay(year, month, req);
        return RespInfo.success();
    }

    /**
     * 点击切换：默认(出勤/休息) -> 出差 -> 请假 -> 默认
     * <p>
     * 前端只需提交 date；当后端切换到请假时，需要提交 leaveType。
     */
    @PostMapping("/day/toggle")
    public RespInfo<AttendanceDayDTO> toggle(@RequestParam int year, @RequestParam int month, @RequestBody AttendanceToggleDayRequest req) {
        return RespInfo.success(attendanceService.toggleDay(year, month, req));
    }

    /**
     * 提交整月
     */
    @PostMapping("/sheet/submit")
    public RespInfo<Void> submit(@RequestParam int year, @RequestParam int month) {
        attendanceService.submit(year, month);
        return RespInfo.success();
    }
}

