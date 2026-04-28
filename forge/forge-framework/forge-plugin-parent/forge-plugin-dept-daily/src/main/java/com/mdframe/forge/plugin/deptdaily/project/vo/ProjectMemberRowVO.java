package com.mdframe.forge.plugin.deptdaily.project.vo;

import lombok.Data;

/**
 * 项目成员展示（联表用户与主组织）
 */
@Data
public class ProjectMemberRowVO {

    private Long userId;
    /** 租户内用户名 */
    private String username;
    /** 真实姓名 */
    private String realName;
    /** 手机号 */
    private String phone;
    /**
     * 主部门/创建部门对应的组织名称（sys_user.create_dept → sys_org.org_name）
     */
    private String deptName;

    /**
     * 在项目成员表中的角色：LEADER / MEMBER 等；
     * 仅按 ID 批量查询简报时为空。
     */
    private String memberRole;
}
