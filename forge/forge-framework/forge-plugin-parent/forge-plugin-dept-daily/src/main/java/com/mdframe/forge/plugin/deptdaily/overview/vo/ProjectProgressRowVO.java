package com.mdframe.forge.plugin.deptdaily.overview.vo;

import lombok.Data;

@Data
public class ProjectProgressRowVO {
    private Long projectId;
    private String projectName;
    /** 类别码 */
    private String projectCategory;
    private Long leaderUserId;
    private String leaderName;
    private String reportYm;
    private String summaryText;
    private String status; // DRAFT/SUBMITTED
    private Long reportId; // 项目月报记录ID（用于详情）
}

