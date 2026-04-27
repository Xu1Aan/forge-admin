package com.mdframe.forge.plugin.deptdaily.attendance.task;

import com.mdframe.forge.plugin.deptdaily.attendance.service.DeptCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 定时预热节假日/调休进库，避免用户首次打开某年月份时才同步打第三方、等待过久。
 * <p>
 * 无登录上下文时与 {@link com.mdframe.forge.plugin.deptdaily.attendance.service.DeptCalendarService} 一致默认租户 1。
 * 多租户场景需后续扩展为遍历租户表后分别 ensure。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeptCalendarWarmupTask {

    private final DeptCalendarService calendarService;

    @Value("${forge.dept-daily.calendar.warmup.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${forge.dept-daily.calendar.warmup.cron:0 0 2 * * ?}")
    public void ensureCurrentAndNextYear() {
        if (!enabled) {
            return;
        }
        try {
            int y = LocalDate.now().getYear();
            calendarService.ensureYearCached(y);
            calendarService.ensureYearCached(y + 1);
        } catch (Exception e) {
            log.warn("节假日日历定时预热失败（可忽略，用户打开月视图时会懒加载拉取）", e);
        }
    }
}
