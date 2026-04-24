package com.mdframe.forge.plugin.system.external.weaver;

import lombok.Data;

/**
 * 同步结果汇总
 */
@Data
public class WeaverSyncResult {
    private Long batchId;
    private String status;

    private int fetchedOrgCount;
    private int fetchedUserCount;

    private int insertedCount;
    private int updatedCount;
    private int disabledCount;
    private int skippedCount;
}

