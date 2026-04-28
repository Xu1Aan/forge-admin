package com.mdframe.forge.plugin.deptdaily.project.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProjectListRowVO {
    private Long id;
    private String projectName;
    private String projectCategory;
    private Long leaderUserId;
    private String leaderName;
    private Integer memberCount;
    private LocalDate startDate;
    private LocalDate planEndDate;
    private String status;
    private LocalDateTime doneAt;
    private Long doneByUserId;
}

