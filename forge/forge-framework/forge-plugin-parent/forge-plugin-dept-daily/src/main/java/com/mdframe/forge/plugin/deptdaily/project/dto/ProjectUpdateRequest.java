package com.mdframe.forge.plugin.deptdaily.project.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectUpdateRequest {
    private Long id;
    private Long deptId;
    private Long officeId;
    private String projectName;
    /** 不传则不改 */
    private String projectCategory;
    private Long leaderUserId;
    private List<Long> memberUserIds;
    private LocalDate startDate;
    private LocalDate planEndDate;
    private String remark;
    /**
     * DRAFT/ACTIVE/DONE/CLOSED（一般不建议直接改，由 finish 接口控制）
     */
    private String status;
}

