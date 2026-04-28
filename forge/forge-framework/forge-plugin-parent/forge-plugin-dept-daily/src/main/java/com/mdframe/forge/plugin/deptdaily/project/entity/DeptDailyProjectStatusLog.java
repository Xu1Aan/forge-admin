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
@TableName("dept_daily_project_status_log")
public class DeptDailyProjectStatusLog extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String fromStatus;

    private String toStatus;

    private String reason;

    private LocalDateTime operatedAt;

    private Long operatedBy;
}

