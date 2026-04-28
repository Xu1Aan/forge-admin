package com.mdframe.forge.plugin.deptdaily.project.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 行模型（按列序读取，避免表头编码差异）。
 * 列顺序：项目名 / 项目类型 / 项目负责人 / 项目组成员
 */
@Data
public class ProjectExcelImportRow {

    @ExcelProperty(index = 0)
    private String projectName;

    @ExcelProperty(index = 1)
    private String projectType;

    @ExcelProperty(index = 2)
    private String leaderName;

    @ExcelProperty(index = 3)
    private String memberNames;
}

