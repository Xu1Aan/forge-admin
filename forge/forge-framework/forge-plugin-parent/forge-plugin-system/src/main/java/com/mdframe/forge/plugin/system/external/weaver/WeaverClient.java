package com.mdframe.forge.plugin.system.external.weaver;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.external.weaver.model.ExternalOrg;
import com.mdframe.forge.plugin.system.external.weaver.model.ExternalUser;
import com.mdframe.forge.plugin.system.external.weaver.model.WeaverSyncPayload;
import com.mdframe.forge.plugin.system.external.weaver.model.WeaverTreeNode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * 泛微(Weaver/Ecology)接口客户端
 */
@Component
@RequiredArgsConstructor
public class WeaverClient {
    public static final String PLATFORM = "weaver";

    private final RestTemplate weaverRestTemplate;
    private final ObjectMapper weaverObjectMapper;
    private final WeaverProperties properties;

    public Snapshot fetchSnapshot() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Weaver 同步未启用：external.weaver.enabled=false");
        }
        if (StringUtils.isBlank(properties.getSyncUrl())) {
            throw new IllegalStateException("Weaver 同步地址未配置：external.weaver.sync-url");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (StringUtils.isNotBlank(properties.getToken())) {
            headers.set(properties.getTokenHeader(), properties.getTokenPrefix() + properties.getToken());
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = weaverRestTemplate.exchange(
                properties.getSyncUrl(),
                HttpMethod.GET,
                entity,
                String.class
        );
        String body = resp.getBody();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Weaver 返回空响应体");
        }

        WeaverSyncPayload payload;
        try {
            payload = weaverObjectMapper.readValue(body, WeaverSyncPayload.class);
        } catch (Exception e) {
            throw new IllegalStateException("解析 Weaver 响应失败: " + e.getMessage(), e);
        }

        NormalizeResult normalized = normalize(payload);
        Snapshot snapshot = new Snapshot();
        snapshot.setRawJson(body);
        snapshot.setRawHash(sha256Hex(body));
        snapshot.setOrgs(normalized.getOrgs());
        snapshot.setUsers(normalized.getUsers());
        return snapshot;
    }

    /**
     * 兼容解析：\n
     * - 若节点包含 org 节点：抽取组织树。\n
     * - 若仅有 user 节点：按 deptId/deptName 生成扁平组织集合。
     */
    public NormalizeResult normalize(WeaverSyncPayload payload) {
        Map<String, ExternalOrg> orgMap = new LinkedHashMap<>();
        List<ExternalUser> users = new ArrayList<>();

        if (payload == null || CollUtil.isEmpty(payload.getChildren())) {
            return new NormalizeResult(new ArrayList<>(), users);
        }

        Deque<WeaverTreeNode> stack = new ArrayDeque<>(payload.getChildren());
        while (!stack.isEmpty()) {
            WeaverTreeNode node = stack.pop();
            if (node.getChildren() != null) {
                for (WeaverTreeNode c : node.getChildren()) {
                    stack.push(c);
                }
            }

            String nodeType = StringUtils.trimToEmpty(node.getNodeType());
            if ("user".equalsIgnoreCase(nodeType) || StringUtils.isNotBlank(node.getAccount()) || StringUtils.isNotBlank(node.getMobile())) {
                ExternalUser u = new ExternalUser();
                u.setExternalUserId(StringUtils.defaultIfBlank(node.getAccount(), node.getId()));
                u.setName(node.getName());
                u.setMobile(node.getMobile());
                u.setEmail(node.getEmail());
                u.setDeptExternalId(node.getDeptId());
                u.setDeptName(node.getDeptName());
                u.setStatus(node.getStatus());
                u.setUpdateTime(node.getUpdateTime() != null ? node.getUpdateTime() : node.getCreateTime());
                if (StringUtils.isNotBlank(u.getExternalUserId())) {
                    users.add(u);
                }

                // fallback org from deptId/deptName
                if (StringUtils.isNotBlank(node.getDeptId())) {
                    orgMap.computeIfAbsent(node.getDeptId(), k -> {
                        ExternalOrg o = new ExternalOrg();
                        o.setExternalOrgId(node.getDeptId());
                        o.setName(StringUtils.defaultIfBlank(node.getDeptName(), node.getDeptId()));
                        o.setStatus("1");
                        return o;
                    });
                }
                continue;
            }

            if ("org".equalsIgnoreCase(nodeType) || StringUtils.isNotBlank(node.getOrgId()) || StringUtils.isNotBlank(node.getOrgName())) {
                String externalOrgId = StringUtils.defaultIfBlank(node.getOrgId(), node.getId());
                if (StringUtils.isBlank(externalOrgId)) {
                    continue;
                }
                ExternalOrg o = orgMap.computeIfAbsent(externalOrgId, k -> new ExternalOrg());
                o.setExternalOrgId(externalOrgId);
                o.setExternalParentId(StringUtils.defaultIfBlank(node.getParentId(), null));
                o.setName(StringUtils.defaultIfBlank(node.getOrgName(), node.getName()));
                o.setStatus(StringUtils.defaultIfBlank(node.getOrgStatus(), node.getStatus()));
                o.setUpdateTime(node.getUpdateTime() != null ? node.getUpdateTime() : node.getCreateTime());
            }
        }

        return new NormalizeResult(new ArrayList<>(orgMap.values()), users);
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("计算SHA-256失败: " + e.getMessage(), e);
        }
    }

    @Data
    public static class Snapshot {
        private String rawJson;
        private String rawHash;
        private List<ExternalOrg> orgs;
        private List<ExternalUser> users;
    }

    @Data
    public static class NormalizeResult {
        private final List<ExternalOrg> orgs;
        private final List<ExternalUser> users;
    }
}

