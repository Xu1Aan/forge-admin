package com.mdframe.forge.starter.auth.sso.weaver;

import com.mdframe.forge.starter.auth.context.WeaverSsoProperties;
import com.mdframe.forge.starter.auth.domain.LoginRequest;
import com.mdframe.forge.starter.auth.domain.LoginResult;
import com.mdframe.forge.starter.auth.service.IAuthService;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 泛微 SSO 登录（authorize -> callback -> 系统签发token）
 */
@Slf4j
@RestController
@RequestMapping("/sso/weaver")
@RequiredArgsConstructor
public class WeaverSsoController {

    /**
     * 固定平台编码：复用现有 oauth2 三方登录策略
     */
    public static final String PLATFORM_CODE = "WEAVER";

    private final WeaverSsoProperties properties;
    private final WeaverSsoStateStore stateStore;
    private final WeaverSsoClient weaverSsoClient;
    private final IAuthService authService;

    /**
     * 跳转到泛微 authorize
     */
    @IgnoreTenant
    @GetMapping("/authorize")
    public RedirectView authorize(@RequestParam(required = false) Long tenantId,
                                  @RequestParam(required = false, defaultValue = "pc") String userClient) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("泛微 SSO 未启用");
        }
        String state = stateStore.createState(tenantId, userClient);

        String url = UriComponentsBuilder
                .fromHttpUrl(joinBaseUrl(properties.getBaseUrl(), "/sso/oauth2.0/authorize"))
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build(true)
                .toUriString();

        log.info("Weaver authorize redirect: tenantId={}, userClient={}, state={}", tenantId, userClient, state);
        RedirectView rv = new RedirectView(url);
        rv.setExposeModelAttributes(false);
        return rv;
    }

    /**
     * 泛微回调：redirect_uri 上拼接 ticket（文档示例 ticket=ST-xxx）
     */
    @IgnoreTenant
    @GetMapping("/callback")
    public RespInfo<LoginResult> callback(@RequestParam(name = "ticket", required = false) String ticket,
                                          @RequestParam(name = "code", required = false) String code,
                                          @RequestParam(required = false) String state,
                                          @RequestParam(required = false) Long tenantId,
                                          @RequestParam(required = false, defaultValue = "pc") String userClient) {
        if (!properties.isEnabled()) {
            return RespInfo.error("泛微 SSO 未启用");
        }

        String realTicket = StringUtils.defaultIfBlank(ticket, code);
        if (StringUtils.isBlank(realTicket)) {
            return RespInfo.error("缺少 ticket 参数");
        }

        WeaverSsoStateStore.Entry entry = null;
        if (StringUtils.isNotBlank(state)) {
            entry = stateStore.consume(state);
            if (entry == null) {
                return RespInfo.error("state无效或已过期，请重新发起登录");
            }
        } else {
            // 兼容模式：直接从前端 /login?ticket=xxx 回调过来（无 state）
            entry = new WeaverSsoStateStore.Entry();
            entry.setTenantId(tenantId);
            entry.setUserClient(StringUtils.defaultIfBlank(userClient, "pc"));
        }

        WeaverSsoClient.AccessTokenResponse token = weaverSsoClient.getAccessToken(realTicket);
        WeaverSsoClient.ProfileResponse profile = weaverSsoClient.getProfile(token.getAccessToken());
        WeaverSsoClient.WeaverUserInfo userInfo = weaverSsoClient.toUserInfo(profile);

        if (StringUtils.isBlank(userInfo.getWorkcode())) {
            return RespInfo.error("泛微返回缺少工号(workcode)，无法登录");
        }

        LoginRequest req = new LoginRequest();
        req.setAuthType(LoginRequest.AUTH_TYPE_OAUTH2);
        req.setUserClient(entry.getUserClient());
        req.setTenantId(entry.getTenantId());
        req.setSocialPlatform(PLATFORM_CODE);
        req.setSocialUuid(userInfo.getWorkcode());
        req.setSocialNickname(StringUtils.defaultIfBlank(userInfo.getLastname(), userInfo.getWorkcode()));
        req.setSocialEmail(userInfo.getEmail());
        req.setPhone(userInfo.getMobile());

        LoginResult result = authService.login(req);
        return RespInfo.success(result);
    }

    private static String joinBaseUrl(String baseUrl, String path) {
        String b = StringUtils.removeEnd(StringUtils.trimToEmpty(baseUrl), "/");
        String p = StringUtils.prependIfMissing(path, "/");
        return b + p;
    }
}

