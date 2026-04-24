package com.mdframe.forge.plugin.system.external.weaver.model;

import lombok.Data;

/**
 * 标准化后的外部用户
 */
@Data
public class ExternalUser {
    private String externalUserId; // workcode / account 等，由上游约定
    /**
     * 泛微人员主键，写入本地 remark 便于对账
     */
    private String resourceId;
    private String name;
    private String mobile;
    /**
     * 上游兜底后的首选手机号/电话
     */
    private String mobileEffective;
    private String telephone;
    private String email;
    private String deptExternalId;
    private String deptName;
    private String status;
    private String idCard;
    /**
     * 原始性别码（如 0/1），由下游映射到本系统 gender
     */
    private String sex;
    private String birthday;
    private String nativePlace;
    private Integer educationLevel;
    private String workStartDate;
    private String companyStartDate;
    private Double workYear;
    private Double companyWorkYear;
    private Long updateTime;
}

