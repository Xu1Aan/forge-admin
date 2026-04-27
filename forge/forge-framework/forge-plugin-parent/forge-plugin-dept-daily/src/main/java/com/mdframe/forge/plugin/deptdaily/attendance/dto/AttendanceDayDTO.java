package com.mdframe.forge.plugin.deptdaily.attendance.dto;

import lombok.Data;

@Data
public class AttendanceDayDTO {
    /**
     * yyyy-MM-dd
     */
    private String date;

    /**
     * 节假日/调休名称（可为空）
     */
    private String name;

    /**
     * 是否休息日（含调休）
     */
    private Boolean offDay;

    /**
     * 默认状态（WORK/REST）
     */
    private String defaultStatus;

    /**
     * 当前填报状态（WORK/REST/TRAVEL/LEAVE）
     */
    private String status;

    /**
     * 请假类型（status=LEAVE）
     */
    private String leaveType;

    private String remark;
}

