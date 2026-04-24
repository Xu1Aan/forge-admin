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
    private Long updateTime;
}

