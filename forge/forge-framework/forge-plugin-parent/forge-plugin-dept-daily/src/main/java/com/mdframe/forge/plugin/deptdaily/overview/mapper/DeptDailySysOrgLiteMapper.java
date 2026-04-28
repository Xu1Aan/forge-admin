package com.mdframe.forge.plugin.deptdaily.overview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailySysOrgLite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeptDailySysOrgLiteMapper extends BaseMapper<DeptDailySysOrgLite> {

    /**
     * 获取 orgId 及其所有子孙组织ID（基于 sys_org.ancestors）。
     * 注意：ancestors 形如 "1,2,3"，因此用 FIND_IN_SET 判断包含。
     */
    @Select("""
            SELECT id
            FROM sys_org
            WHERE tenant_id = #{tenantId}
              AND (id = #{orgId} OR FIND_IN_SET(#{orgId}, ancestors))
            """)
    List<Long> selectDescendantOrgIds(@Param("tenantId") Long tenantId, @Param("orgId") Long orgId);
}

