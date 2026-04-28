package com.mdframe.forge.plugin.deptdaily.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dept_daily_project")
public class DeptDailyProject extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 部门（可为空：不分部门）
     */
    private Long deptId;

    /**
     * 科室（可为空：不分科室）
     */
    private Long officeId;

    private String projectName;

    /**
     * 项目类别码，见 {@link com.mdframe.forge.plugin.deptdaily.project.enums.ProjectCategory}
     */
    private String projectCategory;

    /**
     * 项目负责人（用户ID）
     */
    private Long leaderUserId;

    /**
     * 立项日期
     */
    private LocalDate startDate;

    /**
     * 预计截止日期
     */
    private LocalDate planEndDate;

    /**
     * DRAFT/ACTIVE/DONE/CLOSED
     */
    private String status;

    private LocalDateTime doneAt;

    private Long doneByUserId;

    private String remark;
}

