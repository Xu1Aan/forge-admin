package com.mdframe.forge.plugin.deptdaily.overview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 仅用于 dept-daily 插件内部读取 sys_user 的必要字段，避免依赖 system 插件模块。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class DeptDailySysUserLite extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    private String realName;

    /**
     * 1=正式员工，2=劳务派遣
     */
    private Integer employeeType;

    /**
     * 创建部门（用于按部门/科室过滤，与你们 BaseEntity.createDept 对齐）
     */
    private Long createDept;

    /**
     * 用户状态（0-禁用，1-正常，2-锁定）
     */
    private Integer userStatus;
}

