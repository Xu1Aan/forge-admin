package com.mdframe.forge.plugin.deptdaily.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dept_attendance_sheet")
public class DeptAttendanceSheet extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer year;

    private Integer month;

    /**
     * DRAFT / SUBMITTED
     */
    private String status;

    private LocalDateTime submittedAt;

    private String remark;
}

