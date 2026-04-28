package com.mdframe.forge.plugin.deptdaily.project.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserMonthReportUpsertRequest {
    private String reportYm;     // YYYY-MM
    private Long projectId;
    private String progressText;
    private BigDecimal workDays;
    private String blockers;
    private String nextPlan;
    /**
     * true=提交，false/空=仅保存草稿
     */
    private Boolean submit;
}

