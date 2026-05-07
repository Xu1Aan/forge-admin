package com.mdframe.forge.plugin.deptdaily.overview.vo;

import lombok.Data;

import java.util.List;

/**
 * 考勤一览表（按人、按月）行数据：
 * - days: 下标 1..lengthOfMonth（前端按 1~31 渲染；多余天为空字符串）
 */
@Data
public class AttendanceMonthTableRowVO {
    private Long userId;
    private String username;
    private String realName;
    private Integer employeeType;

    /**
     * 月度填报状态：NONE / DRAFT / SUBMITTED
     */
    private String sheetStatus;

    private Integer workDays;
    private Integer restDays;
    private Integer travelDays;
    private Double leaveDays;

    /**
     * 1..31 的当日状态（WORK/REST/TRAVEL/LEAVE）
     */
    private List<String> days;
}

