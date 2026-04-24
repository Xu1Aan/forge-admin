package com.mdframe.forge.plugin.system.external.weaver.model;

import lombok.Data;

/**
 * 标准化后的外部用户
 */
@Data
public class ExternalUser {
    private String externalUserId; // account/id
    private String name;
    private String mobile;
    private String email;
    private String deptExternalId;
    private String deptName;
    private String status;
    private Long updateTime;
}

