package com.mdframe.forge.plugin.deptdaily.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dept_calendar_day")
public class DeptCalendarDay extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer year;

    /**
     * 日期
     */
    private LocalDate day;

    /**
     * 名称（元旦/周六等）
     */
    private String name;

    /**
     * 是否休息日
     */
    private Integer isOffDay;

    /**
     * 来源：HOLIDAYS / WEEKENDS / WORKDAYS
     */
    private String source;
}

