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
     * 是否公历周六、日（自然周末）
     */
    private Boolean weekend;

    /**
     * 是否调休补班日：自然周末但国家日历要求上班（{@code dept_calendar_day.is_off_day=0}）
     */
    private Boolean compensatoryWorkday;

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

    /**
     * 请假天数（status=LEAVE）：默认1.0，支持0.5
     */
    private Double leaveDays;

    private String remark;
}

