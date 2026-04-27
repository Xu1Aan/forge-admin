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

    private String remark;
}

