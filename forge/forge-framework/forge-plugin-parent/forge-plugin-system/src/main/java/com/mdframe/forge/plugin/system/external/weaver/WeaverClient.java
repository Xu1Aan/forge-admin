package com.mdframe.forge.plugin.system.external.weaver;

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.external.weaver.model.ExternalOrg;
import com.mdframe.forge.plugin.system.external.weaver.model.ExternalUser;
import com.mdframe.forge.plugin.system.external.weaver.model.FlatRow;
import com.mdframe.forge.plugin.system.external.weaver.model.GetUsersInfoResponse;
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
        applyAuthHeaders(headers);
        HttpMethod method = resolveHttpMethod();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = weaverRestTemplate.exchange(
                properties.getSyncUrl(),
                method,
                entity,
                String.class
        );
        String body = resp.getBody();
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Weaver 返回空响应体");
        }

        NormalizeResult normalized;
        if (isFlatDataPayload()) {
            GetUsersInfoResponse flat;
            try {
                flat = weaverObjectMapper.readValue(body, GetUsersInfoResponse.class);
            } catch (Exception e) {
                throw new IllegalStateException("解析泛微 flat 响应失败: " + e.getMessage(), e);
            }
            normalized = normalizeFlat(flat != null ? flat.getData() : null);
        } else {
            WeaverSyncPayload payload;
            try {
                payload = weaverObjectMapper.readValue(body, WeaverSyncPayload.class);
            } catch (Exception e) {
                throw new IllegalStateException("解析 Weaver 响应失败: " + e.getMessage(), e);
            }
            normalized = normalize(payload);
        }
        Snapshot snapshot = new Snapshot();
        snapshot.setRawJson(body);
        snapshot.setRawHash(sha256Hex(body));
        snapshot.setOrgs(normalized.getOrgs());
        snapshot.setUsers(normalized.getUsers());
        snapshot.setFlatSkippedNoExternalUserId(normalized.getSkippedNoExternalUserId());
        return snapshot;
    }

    private void applyAuthHeaders(HttpHeaders headers) {
        if (StringUtils.isNotBlank(properties.getAuthorizationHeaderValue())) {
            String name = StringUtils.defaultIfBlank(properties.getAuthorizationHeaderName(), "Authorization");
            headers.set(name, properties.getAuthorizationHeaderValue());
            return;
        }
        if (StringUtils.isNotBlank(properties.getToken())) {
            headers.set(properties.getTokenHeader(), properties.getTokenPrefix() + properties.getToken());
        }
    }

    private boolean isFlatDataPayload() {
        return WeaverProperties.SyncPayloadType.FLAT_DATA.equalsIgnoreCase(
                StringUtils.trimToEmpty(properties.getSyncPayloadType()));
    }

    private HttpMethod resolveHttpMethod() {
        String m = StringUtils.trimToEmpty(properties.getHttpMethod());
        if ("POST".equalsIgnoreCase(m)) {
            return HttpMethod.POST;
        }
        return HttpMethod.GET;
    }

    /**
     * 将 getUsersInfo 的 data[] 转为 ExternalOrg / ExternalUser
     */
    public NormalizeResult normalizeFlat(List<FlatRow> rows) {
        List<ExternalOrg> orgs = new ArrayList<>();
        List<ExternalUser> users = new ArrayList<>();
        int skippedNoKey = 0;
        if (rows == null || rows.isEmpty()) {
            return new NormalizeResult(orgs, users, 0);
        }
        String disabledHint = StringUtils.trimToEmpty(properties.getOrgDisabledNameContains());
        for (FlatRow row : rows) {
            String kind = StringUtils.trimToEmpty(row.getRowKind());
            if ("department".equalsIgnoreCase(kind)) {
                if (StringUtils.isBlank(row.getDepartmentId())) {
                    continue;
                }
                ExternalOrg o = new ExternalOrg();
                o.setExternalOrgId(row.getDepartmentId().trim());
                o.setExternalParentId(normalizeParentDeptId(row.getSupDepartmentId()));
                o.setName(StringUtils.defaultIfBlank(row.getDepartmentName(), row.getDepartmentId()));
                o.setSort(normalizeSort(row.getShowOrder()));
                o.setStatus(resolveOrgStatusByDept(row, o.getName(), disabledHint));
                o.setUpdateTime(null);
                orgs.add(o);
            } else if ("user".equalsIgnoreCase(kind)) {
                ExternalUser u = new ExternalUser();
                u.setName(row.getLastname());
                u.setMobile(StringUtils.trimToNull(row.getMobile()));
                u.setMobileEffective(StringUtils.trimToNull(row.getMobileEffective()));
                u.setTelephone(StringUtils.trimToNull(row.getTelephone()));
                u.setEmail(StringUtils.trimToNull(row.getEmail()));
                u.setDeptExternalId(StringUtils.trimToNull(row.getDepartmentId()));
                u.setDeptName(StringUtils.trimToNull(row.getDepartmentName()));
                u.setResourceId(StringUtils.trimToNull(row.getResourceId()));
                u.setStatus(toStatusString(row.getStatus()));
                u.setIdCard(StringUtils.trimToNull(row.getCertificatenum()));
                u.setSex(StringUtils.trimToNull(row.getSex()));
                u.setBirthday(StringUtils.trimToNull(row.getBirthday()));
                u.setNativePlace(StringUtils.trimToNull(row.getNativeplace()));
                u.setEducationLevel(row.getEducationlevel());
                u.setWorkStartDate(StringUtils.trimToNull(row.getWorkstartdate()));
                u.setCompanyStartDate(StringUtils.trimToNull(row.getCompanystartdate()));
                u.setWorkYear(row.getWorkyear());
                u.setCompanyWorkYear(row.getCompanyworkyear());
                u.setUpdateTime(row.getModified() != null ? row.getModified() : row.getCreated());
                u.setExternalUserId(resolveWorkcodeExternalId(row));
                if (StringUtils.isNotBlank(u.getExternalUserId())) {
                    users.add(u);
                } else {
                    skippedNoKey++;
                }
            }
        }
        return new NormalizeResult(orgs, users, skippedNoKey);
    }

    private String normalizeParentDeptId(String sup) {
        if (StringUtils.isBlank(sup)) {
            return null;
        }
        String s = sup.trim();
        if ("0".equals(s) || "null".equalsIgnoreCase(s)) {
            return null;
        }
        return s;
    }

    private String resolveOrgStatusByName(String name, String contains) {
        if (StringUtils.isNotBlank(contains) && name != null && name.contains(contains)) {
            return "0";
        }
        return "1";
    }

    private static Integer normalizeSort(Double showOrder) {
        if (showOrder == null) {
            return null;
        }
        // 外部可能是 1.1 / 8.02 等；本系统 sort 为 int，这里做四舍五入
        return (int) Math.round(showOrder);
    }

    /**
     * 部门禁用：优先根据 dep_canceled/canceled，其次根据名称包含封存提示
     */
    private String resolveOrgStatusByDept(FlatRow row, String name, String disabledNameContains) {
        String canceled = StringUtils.trimToEmpty(row.getDepCanceled());
        if ("1".equals(canceled) || "true".equalsIgnoreCase(canceled)) {
            return "0";
        }
        return resolveOrgStatusByName(name, disabledNameContains);
    }

    private static String toStatusString(Object status) {
        if (status == null) {
            return null;
        }
        return String.valueOf(status);
    }

    private String resolveWorkcodeExternalId(FlatRow row) {
        String wc = StringUtils.trimToEmpty(row.getWorkcode());
        if (StringUtils.isNotBlank(wc)) {
            return wc;
        }
        if (properties.isWorkcodeFallbackToResourceId() && StringUtils.isNotBlank(row.getResourceId())) {
            return row.getResourceId().trim();
        }
        return null;
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
            return new NormalizeResult(new ArrayList<>(), users, 0);
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

        return new NormalizeResult(new ArrayList<>(orgMap.values()), users, 0);
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
        /**
         * 扁平行同步：无工号且未启用 resource_id 回退时跳过的 user 行数
         */
        private int flatSkippedNoExternalUserId;
    }

    @Data
    public static class NormalizeResult {
        private final List<ExternalOrg> orgs;
        private final List<ExternalUser> users;
        private final int skippedNoExternalUserId;
    }
}

