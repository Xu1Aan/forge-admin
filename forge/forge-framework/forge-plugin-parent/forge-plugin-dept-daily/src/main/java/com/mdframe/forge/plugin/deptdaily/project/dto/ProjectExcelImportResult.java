package com.mdframe.forge.plugin.deptdaily.project.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectExcelImportResult {

    private int total;
    private int success;
    private int failed;

    /**
     * 成功创建的项目ID（dryRun=false 时才会有值）。
     */
    private List<Long> createdProjectIds = new ArrayList<>();

    /**
     * 已存在并被更新的项目ID（dryRun=false 时才会有值）。
     */
    private List<Long> updatedProjectIds = new ArrayList<>();

    /**
     * 失败明细（含行号与原因）。
     */
    private List<ProjectExcelImportRowError> errors = new ArrayList<>();

    public void addError(int rowNum, String projectName, String message) {
        ProjectExcelImportRowError e = new ProjectExcelImportRowError();
        e.setRowNum(rowNum);
        e.setProjectName(projectName);
        e.setMessage(message);
        errors.add(e);
    }
}

