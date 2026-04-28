package com.mdframe.forge.plugin.deptdaily.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProject;
import com.mdframe.forge.plugin.deptdaily.project.vo.ProjectListRowVO;
import com.mdframe.forge.plugin.deptdaily.project.vo.ProjectMemberRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DeptDailyProjectMapper extends BaseMapper<DeptDailyProject> {

    IPage<ProjectListRowVO> selectProjectPage(
            Page<ProjectListRowVO> page,
            @Param("tenantId") Long tenantId,
            @Param("deptId") Long deptId,
            @Param("officeId") Long officeId,
            @Param("year") Integer year,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("projectCategory") String projectCategory
    );

    List<ProjectMemberRowVO> selectMemberRows(@Param("tenantId") Long tenantId,
                                              @Param("projectId") Long projectId);

    List<ProjectMemberRowVO> selectMemberBriefsByIds(@Param("tenantId") Long tenantId,
                                                   @Param("userIds") Collection<Long> userIds);
}

