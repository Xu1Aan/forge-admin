package com.mdframe.forge.plugin.deptdaily.controller;

import com.mdframe.forge.starter.core.domain.RespInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/dept-daily", "/api/dept-daily"})
public class DeptDailyHealthController {

    @GetMapping("/health")
    public RespInfo<Map<String, Object>> health() {
        return RespInfo.success(Map.of(
                "plugin", "forge-plugin-dept-daily",
                "status", "ok"
        ));
    }
}

