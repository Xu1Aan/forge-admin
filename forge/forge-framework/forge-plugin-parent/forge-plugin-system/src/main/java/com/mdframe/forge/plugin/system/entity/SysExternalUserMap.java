package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外部用户映射表（外部ID→本地sys_user.id）
 */
@Data
@TableName("sys_external_user_map")
public class SysExternalUserMap {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String platform;

    private String externalUserId;
    private Long userId;

    private String loginName;
    private String phoneSnapshot;
    private String emailSnapshot;
    private String deptExternalId;
    private String statusSnapshot;

    private Long lastExternalUpdateTime;
    private Long lastSeenBatchId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

