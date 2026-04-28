package com.mdframe.forge.plugin.deptdaily.project.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 项目类别枚举码（DB/接口层存 code，前端展示中文）
 */
public final class ProjectCategory {

    public static final String INFO_DESIGN = "INFO_DESIGN";
    /** 信息化开发 */
    public static final String INFO_DEV = "INFO_DEV";
    /** 科研项目 */
    public static final String RESEARCH = "RESEARCH";
    /** 电气二次 */
    public static final String ELECTRICAL_SEC = "ELECTRICAL_SEC";
    /** 其他 */
    public static final String OTHER = "OTHER";

    private static final Map<String, String> LABEL_BY_CODE;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(INFO_DESIGN, "信息化设计");
        m.put(INFO_DEV, "信息化开发");
        m.put(RESEARCH, "科研项目");
        m.put(ELECTRICAL_SEC, "电气二次");
        m.put(OTHER, "其他");
        LABEL_BY_CODE = Collections.unmodifiableMap(m);
    }

    private ProjectCategory() {
    }

    public static Map<String, String> allLabels() {
        return LABEL_BY_CODE;
    }

    public static boolean isValid(String code) {
        return code != null && LABEL_BY_CODE.containsKey(code);
    }

    /**
     * 展示用文案；未知或空按「其他」
     */
    public static String labelOf(String code) {
        if (StringUtils.isBlank(code)) {
            return LABEL_BY_CODE.get(OTHER);
        }
        return LABEL_BY_CODE.getOrDefault(code, LABEL_BY_CODE.get(OTHER));
    }
}
