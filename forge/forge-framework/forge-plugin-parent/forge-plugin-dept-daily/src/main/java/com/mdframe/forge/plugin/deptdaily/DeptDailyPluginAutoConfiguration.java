package com.mdframe.forge.plugin.deptdaily;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 部门日常管理插件：自动装配入口（占位）
 */
@Configuration
@MapperScan(basePackages = "com.mdframe.forge.plugin.deptdaily")
public class DeptDailyPluginAutoConfiguration {
}

