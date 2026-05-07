package com.mdframe.forge.plugin.deptdaily.overview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dept_daily_report_setting")
public class DeptDailyReportSetting extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long deptId;

    private Long officeId;

    /**
     * 1=正式员工，2=劳务派遣；NULL=全部
     */
    private Integer employeeType;

    /**
     * YYYY-MM
     */
    private String attendanceStartYm;

    /**
     * YYYY-MM
     */
    private String workReportStartYm;

    /**
     * YYYY-MM
     */
    private String projectReportStartYm;

    /**
     * 考勤导出人员顺序（按姓名，每行一个）
     */
    private String attendanceExportOrder;
}

