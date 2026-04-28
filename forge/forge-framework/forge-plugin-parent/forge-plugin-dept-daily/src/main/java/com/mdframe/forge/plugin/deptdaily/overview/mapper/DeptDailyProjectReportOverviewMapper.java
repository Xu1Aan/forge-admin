package com.mdframe.forge.plugin.deptdaily.overview.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.deptdaily.overview.vo.ProjectProgressRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeptDailyProjectReportOverviewMapper {

    IPage<ProjectProgressRowVO> selectProjectProgressPage(
            Page<ProjectProgressRowVO> page,
            @Param("tenantId") Long tenantId,
            @Param("reportYm") String reportYm,
            @Param("deptId") Long deptId,
            @Param("officeId") Long officeId,
            @Param("keyword") String keyword
    );
}

