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
@TableName("dept_daily_project_member")
public class DeptDailyProjectMember extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long userId;

    /**
     * MEMBER/LEADER/MANAGER(可扩展)
     */
    private String role;

    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    private Integer isActive;
}

