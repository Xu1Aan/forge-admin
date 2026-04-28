package com.mdframe.forge.plugin.deptdaily.overview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.deptdaily.overview.vo.FillStateRowVO;
import com.mdframe.forge.plugin.deptdaily.overview.entity.DeptDailyFillState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeptDailyFillStateMapper extends BaseMapper<DeptDailyFillState> {

    IPage<FillStateRowVO> selectFillStatePage(
            Page<FillStateRowVO> page,
            @Param("tenantId") Long tenantId,
            @Param("module") String module,
            @Param("ym") String ym,
            @Param("deptId") Long deptId,
            @Param("officeId") Long officeId,
            @Param("employeeType") Integer employeeType,
            @Param("status") String status,
            @Param("keyword") String keyword
    );

    /**
     * 批量 upsert（MySQL: ON DUPLICATE KEY UPDATE）
     */
    int upsertBatch(@Param("list") List<DeptDailyFillState> list);
}

