package com.mdframe.forge.plugin.deptdaily.overview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailyUserMonthReportSheet;
import com.mdframe.forge.plugin.deptdaily.overview.vo.UserMonthReportStatRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeptDailyUserMonthReportSheetMapper extends BaseMapper<DeptDailyUserMonthReportSheet> {

    IPage<UserMonthReportStatRowVO> selectUserMonthReportStatPage(
            Page<UserMonthReportStatRowVO> page,
            @Param("tenantId") Long tenantId,
            @Param("reportYm") String reportYm,
            @Param("deptId") Long deptId,
            @Param("officeId") Long officeId,
            @Param("employeeType") Integer employeeType,
            @Param("status") String status,
            @Param("keyword") String keyword
    );
}

