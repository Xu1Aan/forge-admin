package com.mdframe.forge.plugin.system.external.weaver;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashMap;
import java.util.Map;

/**
 * 泛微(Weaver/Ecology)同步配置
 */
@Data
@ConfigurationProperties(prefix = "external.weaver")
public class WeaverProperties {

    /**
     * 响应体形态：tree_children（{children:[]} 树） / flat_data（{data:[]} 扁平行）
     */
    private String syncPayloadType = SyncPayloadType.TREE_CHILDREN;
    /**
     * 是否启用同步
     */
    private boolean enabled = false;

    /**
     * 泛微接口地址（建议配置到返回组织+用户树形JSON的接口）
     */
    private String syncUrl;

    /**
     * 请求超时（毫秒）
     */
    private int timeoutMs = 15000;

    /**
     * 静态Token（如接口使用Bearer/自定义header鉴权）
     */
    private String token;

    /**
     * Token请求头名（默认 Authorization）
     */
    private String tokenHeader = "Authorization";

    /**
     * Token前缀（默认 Bearer）
     */
    private String tokenPrefix = "Bearer ";

    /**
     * 同步所属租户ID（单租户场景可固定；多租户可后续扩展为列表）
     */
    private Long tenantId = 1L;

    /**
     * 完整鉴权头值（不拼接 tokenPrefix，适用于整段放入 Authorization 的场景）。与 token 二选一，优先用本字段。
     */
    private String authorizationHeaderValue;

    /**
     * 与 authorizationHeaderValue 配对的头名，默认 Authorization
     */
    private String authorizationHeaderName = "Authorization";

    /**
     * 拉取快照的 HTTP 方法：GET 或 POST
     */
    private String httpMethod = "GET";

    /**
     * 泛微人员状态码 -> 本系统 userStatus（0/1/2）。未命中时：数字 1 视为正常，其余禁用（与旧逻辑兼容）。
     * 例：1:1,6:0
     */
    private Map<String, Integer> userStatusMap = new HashMap<>();

    /**
     * 部门名称包含该子串时视为禁用（如「已封存」），写入 ExternalOrg.status 为 "0"
     */
    private String orgDisabledNameContains = "已封存";

    /**
     * 组织根节点公司名称：外部顶级部门会挂在该公司节点下（组织管理不再出现多个一级部门）。
     */
    private String rootCompanyName = "四川水发勘测设计研究有限公司";

    /**
     * workcode 为空时是否用 resource_id 作为 external_user_id（需显式开启，避免主键语义变化）
     */
    private boolean workcodeFallbackToResourceId = false;

    public static final class SyncPayloadType {
        public static final String TREE_CHILDREN = "tree_children";
        public static final String FLAT_DATA = "flat_data";

        private SyncPayloadType() {
        }
    }
}

