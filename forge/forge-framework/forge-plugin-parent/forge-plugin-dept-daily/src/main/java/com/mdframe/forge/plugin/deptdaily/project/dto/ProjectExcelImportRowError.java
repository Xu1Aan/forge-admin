package com.mdframe.forge.plugin.deptdaily.project.dto;

import lombok.Data;

@Data
public class ProjectExcelImportRowError {

    /**
     * Excel 行号（从 1 开始，包含表头行）。
     */
    private Integer rowNum;

    private String projectName;

    private String message;
}

