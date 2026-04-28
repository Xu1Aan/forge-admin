package com.mdframe.forge.plugin.deptdaily.overview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailySysUserOrgLite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeptDailySysUserOrgLiteMapper extends BaseMapper<DeptDailySysUserOrgLite> {

    /**
     * 按组织范围取去重用户ID列表（用于统览范围计算）。
     */
    @Select("""
            <script>
            SELECT DISTINCT uo.user_id
            FROM sys_user_org uo
            WHERE uo.tenant_id = #{tenantId}
              AND uo.org_id IN
              <foreach collection="orgIds" item="id" open="(" close=")" separator=",">
                #{id}
              </foreach>
            </script>
            """)
    List<Long> selectDistinctUserIdsByOrgIds(@Param("tenantId") Long tenantId, @Param("orgIds") List<Long> orgIds);
}

