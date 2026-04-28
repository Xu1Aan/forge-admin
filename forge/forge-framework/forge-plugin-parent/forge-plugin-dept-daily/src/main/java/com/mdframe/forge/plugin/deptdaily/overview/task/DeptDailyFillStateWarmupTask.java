package com.mdframe.forge.plugin.deptdaily.overview.task;

import com.mdframe.forge.plugin.deptdaily.overview.service.DeptDailyOverviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 定时刷新 fill_state，保证统览查询走索引、性能稳定。
 * <p>
 * 默认每天凌晨刷新当前月（无登录上下文时与其它任务一致默认租户 1）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeptDailyFillStateWarmupTask {

    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final DeptDailyOverviewService overviewService;

    @Value("${forge.dept-daily.overview.fill-state.warmup.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${forge.dept-daily.overview.fill-state.warmup.cron:0 30 2 * * ?}")
    public void refreshCurrentMonth() {
        if (!enabled) return;
        try {
            String ym = YearMonth.now().format(YM_FMT);
            overviewService.refreshFillState("ATTENDANCE", null, null, null, ym, ym);
            overviewService.refreshFillState("WORK_REPORT", null, null, null, ym, ym);
        } catch (Exception e) {
            log.warn("fill_state 定时刷新失败（可忽略，仍可手动触发刷新）", e);
        }
    }
}

