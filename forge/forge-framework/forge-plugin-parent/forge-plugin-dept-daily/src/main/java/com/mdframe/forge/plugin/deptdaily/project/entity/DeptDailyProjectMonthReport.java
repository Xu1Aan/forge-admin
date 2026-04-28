package com.mdframe.forge.plugin.deptdaily.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dept_daily_project_month_report")
public class DeptDailyProjectMonthReport extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long deptId;

    private Long officeId;

    /**
     * YYYY-MM
     */
    private String reportYm;

    private Long projectId;

    private String summaryText;

    /**
     * GREEN/YELLOW/RED（可选）
     */
    private String overallStatus;

    private String risks;

    private String nextPlan;

    private LocalDateTime submittedAt;

    /**
     * DRAFT/SUBMITTED
     */
    private String status;
}

