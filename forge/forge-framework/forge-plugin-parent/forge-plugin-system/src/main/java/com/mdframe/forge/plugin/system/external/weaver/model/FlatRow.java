package com.mdframe.forge.plugin.system.external.weaver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 泛微 getUsersInfo 等接口返回的扁平行（department / user）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlatRow {

    @JsonProperty("row_kind")
    private String rowKind;

    @JsonProperty("department_id")
    private String departmentId;

    @JsonProperty("sup_department_id")
    private String supDepartmentId;

    @JsonProperty("department_name")
    private String departmentName;

    @JsonProperty("department_mark")
    private String departmentMark;

    /**
     * 部门祖先链（外部格式），用于对账/校验（本系统不强依赖）
     */
    @JsonProperty("all_supdep_id")
    private String allSupdepId;

    /**
     * 部门排序（外部可能是小数）
     */
    @JsonProperty("show_order")
    private Double showOrder;

    /**
     * 部门封存/取消标记（0/1）
     */
    @JsonProperty("dep_canceled")
    private String depCanceled;

    private String loginid;
    private String workcode;
    private String lastname;
    private String sex;
    private String email;
    private String mobile;
    private String telephone;
    private String mobilecall;

    /**
     * 上游已做兜底后的首选联系方式（mobile > mobilecall > telephone）
     */
    @JsonProperty("mobile_effective")
    private String mobileEffective;

    @JsonProperty("certificatenum")
    private String certificatenum;

    @JsonProperty("nativeplace")
    private String nativeplace;

    @JsonProperty("educationlevel")
    private Integer educationlevel;

    @JsonProperty("birthday")
    private String birthday;

    @JsonProperty("workstartdate")
    private String workstartdate;

    @JsonProperty("companystartdate")
    private String companystartdate;

    @JsonProperty("workyear")
    private Double workyear;

    @JsonProperty("companyworkyear")
    private Double companyworkyear;

    @JsonProperty("resource_id")
    private String resourceId;

    @JsonProperty("subcompany_id")
    private String subcompanyId;

    @JsonProperty("jobtitle_id")
    private String jobtitleId;

    @JsonProperty("jobtitle_mark")
    private String jobtitleMark;

    @JsonProperty("jobtitle_name")
    private String jobtitleName;

    /**
     * 泛微可能返回数字 1/6 等
     */
    private Object status;

    private Long modified;
    private Long created;
}
