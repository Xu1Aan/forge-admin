package com.mdframe.forge.plugin.deptdaily.attendance.dto;

import lombok.Data;

import java.util.List;

@Data
public class AttendanceMonthViewDTO {
    private Long sheetId;
    private Integer year;
    private Integer month;
    private String status;
    private List<AttendanceDayDTO> days;
}

