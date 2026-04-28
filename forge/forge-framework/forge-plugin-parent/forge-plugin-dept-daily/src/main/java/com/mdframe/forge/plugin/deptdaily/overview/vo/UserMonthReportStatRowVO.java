package com.mdframe.forge.plugin.deptdaily.overview.vo;

import lombok.Data;

@Data
public class UserMonthReportStatRowVO {
    private Long userId;
    private String username;
    private String realName;
    private Integer employeeType;
    private String reportYm;
    private String status; // DRAFT/SUBMITTED/NONE(补齐)
    private Integer projectCount; // 本月填报项目数
}

