package com.mdframe.forge.starter.auth.context;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 泛微 SSO（OAuth2.0 风格）配置
 */
@Data
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
     * 泛微登录入口完整 URL（优先使用，不再由后端拼接 appid/service/_key）。
     * <p>
     * 示例：http://10.20.123.5/wui/index.html#/?appid=one&service=http://10.20.149.205:3000
     */
    private String loginUrl;

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
     * 泛微 wui 入口的 service 参数（如：http://localhost:3000/ 或 http://localhost:3000/login）。
     * <p>
     * 为空时默认使用 redirectUri
     */
    private String serviceUrl;

    /**
     * 泛微 wui 入口 _key（如：704693）。若未使用可为空。
     */
    private String wuiKey;

    /**
     * state 有效期（秒）
     */
    private long stateTtlSeconds = 300;
}

