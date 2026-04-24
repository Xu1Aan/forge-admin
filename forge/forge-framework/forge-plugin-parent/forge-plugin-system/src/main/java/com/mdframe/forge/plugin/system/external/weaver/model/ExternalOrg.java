package com.mdframe.forge.plugin.system.external.weaver.model;

import lombok.Data;

/**
 * 标准化后的外部组织
 */
@Data
public class ExternalOrg {
    private String externalOrgId;
    private String externalParentId;
    private String name;
    private String status;
    /**
     * 排序（外部 show_order 转为整数）
     */
    private Integer sort;
    private Long updateTime;
}

