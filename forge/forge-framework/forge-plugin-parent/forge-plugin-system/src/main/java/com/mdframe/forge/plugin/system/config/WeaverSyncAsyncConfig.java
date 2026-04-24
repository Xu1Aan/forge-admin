package com.mdframe.forge.plugin.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启用异步执行泛微全量同步（与 Excel 等 starter 中的 {@code @EnableAsync} 并存无害）。
 */
@Configuration
@EnableAsync
public class WeaverSyncAsyncConfig {
}
