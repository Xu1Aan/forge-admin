package com.mdframe.forge.plugin.deptdaily.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProject;
import com.mdframe.forge.plugin.deptdaily.project.vo.ProjectListRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeptDailyProjectMapper extends BaseMapper<DeptDailyProject> {

    IPage<ProjectListRowVO> selectProjectPage(
            Page<ProjectListRowVO> page,
            @Param("tenantId") Long tenantId,
            @Param("deptId") Long deptId,
            @Param("officeId") Long officeId,
            @Param("year") Integer year,
            @Param("keyword") String keyword,
            @Param("status") String status
    );
}

