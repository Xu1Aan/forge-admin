package com.mdframe.forge.plugin.system.external.weaver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 泛微返回的树节点（兼容用户/组织混合树）
 * <p>
 * 仅定义本同步用到的字段，未定义字段忽略。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeaverTreeNode {
    private String nodeType; // user/org/...

    // user fields
    private String account;
    private String id;
    private String name;
    private String mobile;
    private String email;
    private String deptId;
    private String deptName;
    private String status;
    private Long updateTime;
    private Long createTime;
    private String managerId;
    private String managerName;

    // org fields (if provided)
    @JsonProperty("orgId")
    private String orgId;
    @JsonProperty("orgName")
    private String orgName;
    @JsonProperty("parentId")
    private String parentId;
    @JsonProperty("orgStatus")
    private String orgStatus;

    private List<WeaverTreeNode> children;
}

