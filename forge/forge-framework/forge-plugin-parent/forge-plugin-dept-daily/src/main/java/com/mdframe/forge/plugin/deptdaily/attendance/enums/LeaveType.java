package com.mdframe.forge.plugin.deptdaily.attendance.enums;

/**
 * 请假类型枚举码（DB/接口层存 code，前端展示中文）
 */
public final class LeaveType {
    public static final String SICK = "SICK"; // 病假
    public static final String PERSONAL = "PERSONAL"; // 事假
    public static final String PUBLIC = "PUBLIC"; // 公休
    public static final String MARRIAGE = "MARRIAGE"; // 婚假
    public static final String BEREAVEMENT = "BEREAVEMENT"; // 丧假
    public static final String ANNUAL = "ANNUAL"; // 年假
    public static final String MATERNITY = "MATERNITY"; // 产假
    public static final String NURSING_CARE = "NURSING_CARE"; // 护理假
    public static final String FAMILY_PLANNING = "FAMILY_PLANNING"; // 计划生育假
    public static final String HOME_VISIT = "HOME_VISIT"; // 探亲假
    public static final String WORK_INJURY = "WORK_INJURY"; // 工伤假

    private LeaveType() {
    }
}

