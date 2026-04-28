package com.mdframe.forge.plugin.deptdaily.project.dto;

import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectPageQuery extends PageQuery {
    private Long deptId;
    private Long officeId;
    private Integer year;
    private String keyword;
    private String status;
    /** 按项目类别筛选 */
    private String projectCategory;
}

