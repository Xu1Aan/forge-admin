package com.mdframe.forge.plugin.system.external.weaver;

import com.mdframe.forge.plugin.system.entity.SysExternalSyncBatch;
import com.mdframe.forge.plugin.system.mapper.SysExternalSyncBatchMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 与 {@link WeaverSyncService} 配合：在独立事务中先提交 running 批次，供异步任务执行；失败时在另一事务中写回状态。
 */
@Component
@RequiredArgsConstructor
public class WeaverSyncTransactionHelper {
    private static final String PLATFORM = WeaverClient.PLATFORM;

    private final SysExternalSyncBatchMapper batchMapper;
    private final WeaverProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public SysExternalSyncBatch insertRunningBatch(String triggerType) {
        Long tenantId = properties.getTenantId();
        if (tenantId == null) {
            tenantId = 1L;
        }
        SysExternalSyncBatch batch = new SysExternalSyncBatch();
        batch.setTenantId(tenantId);
        batch.setPlatform(PLATFORM);
        batch.setTriggerType(StringUtils.defaultIfBlank(triggerType, "schedule"));
        batch.setStatus("running");
        batch.setStartedAt(LocalDateTime.now());
        batchMapper.insert(batch);
        return batch;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markBatchFailed(long batchId, String errorMessage) {
        SysExternalSyncBatch b = batchMapper.selectById(batchId);
        if (b == null) {
            return;
        }
        b.setStatus("failed");
        b.setErrorMessage(StringUtils.abbreviate(StringUtils.defaultString(errorMessage), 60000));
        b.setEndedAt(LocalDateTime.now());
        batchMapper.updateById(b);
    }
}
