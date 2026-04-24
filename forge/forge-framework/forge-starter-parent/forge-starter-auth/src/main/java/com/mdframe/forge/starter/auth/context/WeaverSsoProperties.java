package com.mdframe.forge.starter.auth.context;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 泛微 SSO（OAuth2.0 风格）配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "forge.sso.weaver")
public class WeaverSsoProperties {

    /**
     * 是否启用泛微 SSO 登录
     */
    private boolean enabled = false;

    /**
     * OA SSO 基础地址，如：http://127.0.0.1:8809 （不含末尾 /）
     */
    private String baseUrl;

    /**
     * 应用标识 client_id
     */
    private String clientId;

    /**
     * 应用密钥 client_secret
     */
    private String clientSecret;

    /**
     * 回调地址 redirect_uri（需与 authorize 时一致，且一般需要 urlencode）
     */
    private String redirectUri;

    /**
     * state 有效期（秒）
     */
    private long stateTtlSeconds = 300;
}

