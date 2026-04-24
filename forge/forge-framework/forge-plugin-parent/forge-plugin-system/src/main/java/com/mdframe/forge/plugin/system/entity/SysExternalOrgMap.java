package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外部组织映射表（外部ID→本地sys_org.id）
 */
@Data
@TableName("sys_external_org_map")
public class SysExternalOrgMap {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String platform;

    private String externalOrgId;
    private String externalParentId;

    private Long orgId;

    private String nameSnapshot;
    private String statusSnapshot;
    private Long lastExternalUpdateTime;
    private Long lastSeenBatchId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

