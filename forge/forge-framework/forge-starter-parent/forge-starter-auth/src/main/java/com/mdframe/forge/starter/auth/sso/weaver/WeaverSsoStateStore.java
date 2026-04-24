package com.mdframe.forge.starter.auth.sso.weaver;

import com.mdframe.forge.starter.auth.context.WeaverSsoProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 泛微 SSO state 存储（默认本地内存）。
 * <p>
 * 注意：多实例部署建议替换为 Redis 实现。
 */
@Component
@RequiredArgsConstructor
public class WeaverSsoStateStore {

    private final WeaverSsoProperties properties;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public String createState(Long tenantId, String userClient) {
        cleanupExpired();
        String state = java.util.UUID.randomUUID().toString().replace("-", "");
        Entry e = new Entry();
        e.setTenantId(tenantId);
        e.setUserClient(StringUtils.defaultIfBlank(userClient, "pc"));
        e.setExpireAtEpochMs(System.currentTimeMillis() + properties.getStateTtlSeconds() * 1000L);
        store.put(state, e);
        return state;
    }

    /**
     * 校验并消费 state（一次性）。
     */
    public Entry consume(String state) {
        cleanupExpired();
        if (StringUtils.isBlank(state)) {
            return null;
        }
        Entry e = store.remove(state);
        if (e == null) {
            return null;
        }
        if (e.getExpireAtEpochMs() < System.currentTimeMillis()) {
            return null;
        }
        return e;
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        // 轻量清理：只在请求时做一次遍历，避免引入定时任务
        for (Map.Entry<String, Entry> it : store.entrySet()) {
            if (it.getValue() == null || it.getValue().getExpireAtEpochMs() < now) {
                store.remove(it.getKey());
            }
        }
    }

    @Data
    public static class Entry {
        private Long tenantId;
        private String userClient;
        private long expireAtEpochMs;

        public Instant getExpireAt() {
            return Instant.ofEpochMilli(expireAtEpochMs);
        }
    }
}

