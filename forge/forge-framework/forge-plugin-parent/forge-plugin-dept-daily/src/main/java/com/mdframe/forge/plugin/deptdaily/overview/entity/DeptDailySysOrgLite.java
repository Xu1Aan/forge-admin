package com.mdframe.forge.plugin.deptdaily.overview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 仅用于 dept-daily 插件内部读取 sys_org 的必要字段（组织树）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
public class DeptDailySysOrgLite extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long parentId;

    /**
     * 祖级编码（逗号分隔，如：1,2,3）
     */
    private String ancestors;
}

