package com.mdframe.forge.plugin.deptdaily.project.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectCreateRequest {
    private Long deptId;
    private Long officeId;
    private String projectName;
    private Long leaderUserId;
    private List<Long> memberUserIds;
    private LocalDate startDate;
    private LocalDate planEndDate;
    private String remark;
}

