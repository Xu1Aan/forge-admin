package com.mdframe.forge.plugin.deptdaily.project.dto;

import lombok.Data;

@Data
public class ProjectMonthReportUpsertRequest {
    private String reportYm;       // YYYY-MM
    private Long projectId;
    private String summaryText;
    private String overallStatus;  // GREEN/YELLOW/RED
    private String risks;
    private String nextPlan;
    private Boolean submit;
}

