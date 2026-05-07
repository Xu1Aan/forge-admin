package com.mdframe.forge.plugin.deptdaily.attendance.dto;

import lombok.Data;

@Data
public class AttendanceUpdateDayRequest {
    /**
     * yyyy-MM-dd
     */
    private String date;

    /**
     * WORK/REST/TRAVEL/LEAVE
     */
    private String status;

    /**
     * status=LEAVE 时必填
     */
    private String leaveType;

    /**
     * status=LEAVE 时可选：请假天数（默认1.0，支持0.5）
     */
    private Double leaveDays;

    private String remark;
}

