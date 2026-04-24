package com.mdframe.forge.plugin.system.external.weaver;

import com.mdframe.forge.starter.job.annotation.ScheduledJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 泛微用户/组织同步定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeaverSyncJob {
    private final WeaverSyncService syncService;

    @ScheduledJob(
            name = "weaverUserOrgSync",
            group = "EXTERNAL",
            cron = "0 0 2 * * ?",
            description = "泛微用户/组织全量差分同步（默认每日2点）",
            enabled = true
    )
    public void run() {
        try {
            syncService.syncFull("schedule");
        } catch (Exception e) {
            log.error("Weaver 定时同步执行失败", e);
            // 让任务框架记录失败即可
            throw e;
        }
    }
}

