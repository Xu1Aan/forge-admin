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
@TableName("dept_attendance_item")
public class DeptAttendanceItem extends TenantEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long sheetId;

    private Long userId;

    private LocalDate workDate;

    /**
     * WORK / REST / TRAVEL / LEAVE
     */
    private String dayStatus;

    /**
     * dayStatus=LEAVE 时必填
     */
    private String leaveType;

    /**
     * 请假天数（dayStatus=LEAVE）
     * - null 表示默认 1.0
     * - 支持 0.5 / 1.0
     */
    private Double leaveDays;

    private String remark;
}

