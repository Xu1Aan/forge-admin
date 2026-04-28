package com.mdframe.forge.plugin.deptdaily.overview.dto;

import lombok.Data;

/**
 * 手动刷新填报状态（fill_state）。
 */
@Data
public class FillStateRefreshRequest {
    /**
     * ATTENDANCE / WORK_REPORT （PROJECT_REPORT 预留）
     */
    private String module;

    private Long deptId;

    private Long officeId;

    /**
     * 1=正式员工，2=劳务派遣；NULL=全部
     */
    private Integer employeeType;

    /**
     * 刷新开始月 YYYY-MM（为空则从 setting 取）
     */
    private String startYm;

    /**
     * 刷新结束月 YYYY-MM（为空则默认为当前月）
     */
    private String endYm;
}

