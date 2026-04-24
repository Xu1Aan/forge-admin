package com.mdframe.forge.plugin.system.external.weaver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 泛微同步接口返回体（示例：{ children: [...] }）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeaverSyncPayload {
    private List<WeaverTreeNode> children;
}

