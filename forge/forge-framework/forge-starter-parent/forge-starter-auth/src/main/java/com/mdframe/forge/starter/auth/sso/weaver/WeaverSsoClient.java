package com.mdframe.forge.starter.auth.sso.weaver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.auth.context.WeaverSsoProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 泛微 SSO 接口客户端（authorize/accessToken/profile）
 */
@Component
@RequiredArgsConstructor
public class WeaverSsoClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WeaverSsoProperties properties;

    public AccessTokenResponse getAccessToken(String ticket) {
        ensureEnabledAndConfigured();
        if (StringUtils.isBlank(ticket)) {
            throw new IllegalArgumentException("ticket不能为空");
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(joinBaseUrl(properties.getBaseUrl(), "/sso/oauth2.0/accessToken"))
                .queryParam("client_id", properties.getClientId())
                .queryParam("client_secret", properties.getClientSecret())
                .queryParam("grant_type", "authorization_code")
                .queryParam("code", ticket)
                .queryParam("redirect_uri", properties.getRedirectUri())
                .build(true)
                .toUriString();

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        String body = resp.getBody();
        if (StringUtils.isBlank(body)) {
            throw new IllegalStateException("泛微 accessToken 返回空响应体");
        }

        try {
            AccessTokenResponse r = objectMapper.readValue(body, AccessTokenResponse.class);
            if (r.getStatus() != null && r.getStatus() == 400) {
                throw new IllegalStateException("泛微 accessToken 错误: " + safeMsg(r.getMsg()) + " (code=" + safeMsg(r.getCode()) + ")");
            }
            if (StringUtils.isBlank(r.getAccessToken())) {
                throw new IllegalStateException("泛微 accessToken 响应缺少 access_token: " + body);
            }
            return r;
        } catch (Exception e) {
            throw new IllegalStateException("解析泛微 accessToken 响应失败: " + e.getMessage(), e);
        }
    }

    public ProfileResponse getProfile(String accessToken) {
        ensureEnabledAndConfigured();
        if (StringUtils.isBlank(accessToken)) {
            throw new IllegalArgumentException("accessToken不能为空");
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(joinBaseUrl(properties.getBaseUrl(), "/sso/oauth2.0/profile"))
                .queryParam("access_token", accessToken)
                .build(true)
                .toUriString();

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        String body = resp.getBody();
        if (StringUtils.isBlank(body)) {
            throw new IllegalStateException("泛微 profile 返回空响应体");
        }

        try {
            ProfileResponse r = objectMapper.readValue(body, ProfileResponse.class);
            if (r.getStatus() != null && r.getStatus() == 400) {
                throw new IllegalStateException("泛微 profile 错误: " + safeMsg(r.getMsg()) + " (code=" + safeMsg(r.getCode()) + ")");
            }
            if (r.getAttributes() == null) {
                throw new IllegalStateException("泛微 profile 响应缺少 attributes: " + body);
            }
            return r;
        } catch (Exception e) {
            throw new IllegalStateException("解析泛微 profile 响应失败: " + e.getMessage(), e);
        }
    }

    public WeaverUserInfo toUserInfo(ProfileResponse profile) {
        ProfileResponse.Attributes a = profile.getAttributes();
        WeaverUserInfo u = new WeaverUserInfo();
        u.setWorkcode(trimToNull(a.getWorkcode()));
        u.setLastname(trimToNull(a.getLastname()));
        u.setMobile(trimToNull(a.getMobile()));
        u.setEmail(trimToNull(a.getEmail()));
        u.setWeaverId(a.getId());
        u.setLoginId(trimToNull(profile.getId()));
        u.setRawAttributes(asJson(a));
        return u;
    }

    private String asJson(Object obj) {
        try {
            JsonNode node = objectMapper.valueToTree(obj);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    private void ensureEnabledAndConfigured() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("泛微 SSO 未启用：forge.sso.weaver.enabled=false");
        }
        if (StringUtils.isBlank(properties.getBaseUrl())) {
            throw new IllegalStateException("泛微 SSO base-url 未配置：forge.sso.weaver.base-url");
        }
        if (StringUtils.isBlank(properties.getClientId())) {
            throw new IllegalStateException("泛微 SSO client-id 未配置：forge.sso.weaver.client-id");
        }
        if (StringUtils.isBlank(properties.getClientSecret())) {
            throw new IllegalStateException("泛微 SSO client-secret 未配置：forge.sso.weaver.client-secret");
        }
        if (StringUtils.isBlank(properties.getRedirectUri())) {
            throw new IllegalStateException("泛微 SSO redirect-uri 未配置：forge.sso.weaver.redirect-uri");
        }
    }

    private static String joinBaseUrl(String baseUrl, String path) {
        String b = StringUtils.removeEnd(StringUtils.trimToEmpty(baseUrl), "/");
        String p = StringUtils.prependIfMissing(path, "/");
        return b + p;
    }

    private static String trimToNull(String s) {
        return StringUtils.trimToNull(s);
    }

    private static String safeMsg(String s) {
        return s == null ? "" : s;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccessTokenResponse {
        private Integer status;
        private String code;
        private String msg;
        @JsonProperty("access_token")
        private String accessToken;
        private Long expires;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfileResponse {
        private Integer status;
        private String code;
        private String msg;
        /**
         * 顶层 id（示例：yhm01）
         */
        private String id;
        private Attributes attributes;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Attributes {
            private Long id;
            private String workcode;
            private String lastname;
            private String mobile;
            private String email;
        }
    }

    @Data
    public static class WeaverUserInfo {
        /**
         * 工号（推荐作为 socialUuid）
         */
        private String workcode;
        private String lastname;
        private String mobile;
        private String email;
        /**
         * 泛微内部数值 id（attributes.id）
         */
        private Long weaverId;
        /**
         * 顶层 loginId（profile.id）
         */
        private String loginId;
        /**
         * attributes 原始 JSON（便于排障）
         */
        private String rawAttributes;
    }
}

