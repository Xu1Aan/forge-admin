package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外部同步批次日志表
 */
@Data
@TableName("sys_external_sync_batch")
public class SysExternalSyncBatch {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String platform;
    private String triggerType;
    private String status;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    private String rawSnapshotHash;

    private Integer fetchedOrgCount;
    private Integer fetchedUserCount;

    private Integer insertedCount;
    private Integer updatedCount;
    private Integer disabledCount;
    private Integer skippedCount;

    private String errorMessage;
}

