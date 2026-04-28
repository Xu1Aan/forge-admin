package com.mdframe.forge.plugin.deptdaily.overview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仅用于 dept-daily 插件内部读取 sys_user_org 的必要字段，避免依赖 system 插件模块。
 */
@Data
@TableName("sys_user_org")
public class DeptDailySysUserOrgLite {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long userId;

    private Long orgId;

    private Integer isMain;

    private LocalDateTime createTime;
}

