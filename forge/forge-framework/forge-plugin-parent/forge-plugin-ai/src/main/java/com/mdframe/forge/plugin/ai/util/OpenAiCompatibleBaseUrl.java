package com.mdframe.forge.plugin.ai.util;

import org.springframework.util.StringUtils;

/**
 * Spring AI {@code OpenAiApi} 会在 {@code baseUrl} 后拼接 {@code /v1/chat/completions} 等路径。
 * 若配置的 Base URL 已带 {@code /v1}（与官方文档「完整 endpoint」表述一致时常见），会拼成 {@code .../v1/v1/...} 导致 404。
 */
public final class OpenAiCompatibleBaseUrl {

    private OpenAiCompatibleBaseUrl() {
    }

    /**
     * 去掉尾部 {@code /} 与多余的 {@code /v1}，使与 OpenAI 兼容网关能正确拼接。
     */
    public static String normalize(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        String u = baseUrl.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        if (u.endsWith("/v1")) {
            u = u.substring(0, u.length() - 3);
            while (u.endsWith("/")) {
                u = u.substring(0, u.length() - 1);
            }
        }
        return u;
    }
}
