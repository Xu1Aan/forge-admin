package com.mdframe.forge.plugin.deptdaily.project.dto;

import lombok.Data;

@Data
public class ProjectFinishRequest {
    /**
     * true=标记完成(DONE)，false=恢复为进行中(ACTIVE)
     */
    private Boolean done;
    /**
     * 可选：原因说明
     */
    private String reason;
}

