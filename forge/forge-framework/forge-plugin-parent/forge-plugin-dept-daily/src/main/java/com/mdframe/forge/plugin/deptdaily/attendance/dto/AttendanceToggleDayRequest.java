package com.mdframe.forge.plugin.deptdaily.attendance.dto;

import lombok.Data;

@Data
public class AttendanceToggleDayRequest {
    /**
     * yyyy-MM-dd
     */
    private String date;

    /**
     * 当后端切换到 LEAVE 时需要；其它状态可不传
     */
    private String leaveType;

    /**
     * 可选备注
     */
    private String remark;
}

