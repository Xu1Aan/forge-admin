package com.mdframe.forge.plugin.system.external.weaver;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 泛微(Weaver/Ecology)同步配置
 */
@Data
@ConfigurationProperties(prefix = "external.weaver")
public class WeaverProperties {
    /**
     * 是否启用同步
     */
    private boolean enabled = false;

    /**
     * 泛微接口地址（建议配置到返回组织+用户树形JSON的接口）
     */
    private String syncUrl;

    /**
     * 请求超时（毫秒）
     */
    private int timeoutMs = 15000;

    /**
     * 静态Token（如接口使用Bearer/自定义header鉴权）
     */
    private String token;

    /**
     * Token请求头名（默认 Authorization）
     */
    private String tokenHeader = "Authorization";

    /**
     * Token前缀（默认 Bearer）
     */
    private String tokenPrefix = "Bearer ";

    /**
     * 同步所属租户ID（单租户场景可固定；多租户可后续扩展为列表）
     */
    private Long tenantId = 1L;
}

