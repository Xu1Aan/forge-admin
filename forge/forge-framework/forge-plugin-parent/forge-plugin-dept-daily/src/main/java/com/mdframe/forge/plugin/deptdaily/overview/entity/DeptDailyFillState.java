package com.mdframe.forge.plugin.deptdaily.overview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dept_daily_fill_state")
public class DeptDailyFillState extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long deptId;

    private Long officeId;

    /**
     * 1=正式员工，2=劳务派遣
     */
    private Integer employeeType;

    /**
     * ATTENDANCE/WORK_REPORT/PROJECT_REPORT
     */
    private String module;

    private Integer year;

    private Integer month;

    private String ym;

    private Long userId;

    /**
     * NONE/DRAFT/SUBMITTED
     */
    private String status;

    private LocalDateTime lastCalcTime;
}

