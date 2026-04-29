package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 用户新增/修改DTO
 */
@Data
public class SysUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（修改时必传）
     */
    private Long id;

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 用户名（租户内唯一）
     */
    private String username;

    /**
     * 用户真实姓名
     */
    private String realName;

    /**
     * 用户类型（0-系统管理员，1-租户管理员，2-普通用户）
     */
    private Integer userType;

    /**
     * 用户触点（app/pc/h5/wechat）
     */
    private String userClient;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 工号
     */
    private String workcode;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 性别（0-未知，1-男，2-女）
     */
    private Integer gender;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 籍贯
     */
    private String nativePlace;

    /**
     * 教育程度（外部码值）
     */
    private Integer educationLevel;

    /**
     * 参加工作日期
     */
    private LocalDate workStartDate;

    /**
     * 入司日期
     */
    private LocalDate companyStartDate;

    /**
     * 工作年限（年）
     */
    private BigDecimal workYear;

    /**
     * 司龄（年）
     */
    private BigDecimal companyWorkYear;

    /**
     * 密码（新增时必传）
     */
    private String password;

    /**
     * 用户状态（0-禁用，1-正常，2-锁定）
     */
    private Integer userStatus;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 行政区划编码
     */
    private String regionCode;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建部门
     */
    private Long createDept;
}
