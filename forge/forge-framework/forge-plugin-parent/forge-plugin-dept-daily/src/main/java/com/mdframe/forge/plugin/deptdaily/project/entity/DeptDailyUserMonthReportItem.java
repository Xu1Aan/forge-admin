package com.mdframe.forge.plugin.deptdaily.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dept_daily_user_month_report_item")
public class DeptDailyUserMonthReportItem extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long deptId;

    private Long officeId;

    /**
     * YYYY-MM
     */
    private String reportYm;

    private Long projectId;

    private Long userId;

    private String progressText;

    private BigDecimal workDays;

    private String blockers;

    private String nextPlan;

    private LocalDateTime submittedAt;

    /**
     * DRAFT/SUBMITTED
     */
    private String status;
}

