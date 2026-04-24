package com.mdframe.forge.plugin.system.external.weaver;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步执行泛微同步，避免自调用使 {@link org.springframework.transaction.annotation.Transactional} 失效，须单独成 Bean。
 */
@Slf4j
@Component
public class WeaverSyncAsyncExecutor {
    private final WeaverSyncService weaverSyncService;

    public WeaverSyncAsyncExecutor(@Lazy @NonNull WeaverSyncService weaverSyncService) {
        this.weaverSyncService = weaverSyncService;
    }

    @Async
    public void runAsyncWeaverSync(Long batchId) {
        try {
            weaverSyncService.executeWeaverSyncByIdAfterSubmit(batchId);
        } catch (Exception e) {
            log.error("Weaver 异步任务异常结束 batchId={}", batchId, e);
        }
    }
}
