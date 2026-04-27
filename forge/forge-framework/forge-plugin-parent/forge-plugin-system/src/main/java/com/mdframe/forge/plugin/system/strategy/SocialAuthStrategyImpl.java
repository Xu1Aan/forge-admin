package com.mdframe.forge.plugin.system.strategy;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.service.ISysConfigService;
import com.mdframe.forge.starter.auth.domain.LoginRequest;
import com.mdframe.forge.starter.auth.enums.AuthType;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.social.context.SocialProperties;
import com.mdframe.forge.starter.social.domain.entity.SysUserSocial;
import com.mdframe.forge.starter.social.mapper.SysUserSocialMapper;
import com.mdframe.forge.starter.social.service.ISocialUserService;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 三方登录认证策略实现
 * 继承AbstractAuthStrategy复用通用认证逻辑
 */
@Slf4j
@Component
public class SocialAuthStrategyImpl extends AbstractAuthStrategy {

    @Autowired
    private ISocialUserService socialUserService;

    @Autowired
    private SocialProperties socialProperties;

    @Autowired
    private ISysConfigService sysConfigService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysUserSocialMapper userSocialMapper;

    @Override
    protected void validateRequest(LoginRequest request) {
        if (StrUtil.isBlank(request.getSocialPlatform())) {
            throw new RuntimeException("三方平台类型不能为空");
        }
        if (StrUtil.isBlank(request.getSocialUuid())) {
            throw new RuntimeException("三方用户唯一标识不能为空");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected LoginUser doAuthenticate(LoginRequest request) {
        String platform = request.getSocialPlatform();
        String socialUuid = request.getSocialUuid();
        String socialNickname = request.getSocialNickname();
        String socialAvatar = request.getSocialAvatar();
        String socialEmail = request.getSocialEmail();
        Long tenantId = request.getTenantId();

        log.info("三方登录开始: platform={}, uuid={}", platform, socialUuid);

        // 1. 查询是否已绑定
        SysUserSocial userSocial = socialUserService.selectByPlatformAndUuid(platform, socialUuid);
        if (userSocial != null) {
            // 已绑定，通过userId查询用户后，再用username加载LoginUser
            Long userId = userSocial.getUserId();
            log.info("三方登录成功（已绑定）: platform={}, uuid={}, userId={}", platform, socialUuid, userId);

            SysUser sysUser = userMapper.selectById(userId);
            if (sysUser == null) {
                // 绑定脏数据：sys_user_social 存在，但 user_id 对应用户已不存在
                // 清理后继续走未绑定分支，按 workcode/手机号/邮箱 自动匹配并重绑
                log.warn("检测到三方绑定脏数据，清理后重试匹配: platform={}, uuid={}, userId={}", platform, socialUuid, userId);
                userSocialMapper.deleteById(userSocial.getId());
            } else {
                LoginUser loginUser = userLoadService.loadUserByUsername(sysUser.getUsername(), tenantId);
                if (loginUser == null) {
                    throw new RuntimeException("加载用户信息失败");
                }
                return loginUser;
            }
        }

        // 2. 未绑定：优先尝试匹配“已导入用户”，匹配到则直接绑定，避免自动创建新用户
        // 约定：Weaver SSO 的 socialUuid 通常为工号(workcode)
        SysUser existingUser = null;
        if (StrUtil.isNotBlank(socialUuid)) {
            LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
            w.eq(SysUser::getWorkcode, socialUuid);
            if (tenantId != null) {
                w.eq(SysUser::getTenantId, tenantId);
            }
            w.last("limit 1");
            existingUser = userMapper.selectOne(w);
        }
        if (existingUser == null && StrUtil.isNotBlank(request.getPhone())) {
            LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
            w.eq(SysUser::getPhone, request.getPhone());
            if (tenantId != null) {
                w.eq(SysUser::getTenantId, tenantId);
            }
            w.last("limit 1");
            existingUser = userMapper.selectOne(w);
        }
        if (existingUser == null && StrUtil.isNotBlank(socialEmail)) {
            LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
            w.eq(SysUser::getEmail, socialEmail);
            if (tenantId != null) {
                w.eq(SysUser::getTenantId, tenantId);
            }
            w.last("limit 1");
            existingUser = userMapper.selectOne(w);
        }

        if (existingUser != null) {
            log.info("三方登录发现匹配的已存在用户，执行自动绑定: platform={}, uuid={}, userId={}, username={}",
                    platform, socialUuid, existingUser.getId(), existingUser.getUsername());

            AuthUser au = new AuthUser();
            au.setUuid(socialUuid);
            au.setUsername(existingUser.getUsername());
            au.setNickname(socialNickname);
            au.setAvatar(socialAvatar);
            au.setEmail(socialEmail);
            socialUserService.bindSocialUser(existingUser.getId(), au, platform, tenantId);

            LoginUser loginUser = userLoadService.loadUserByUsername(existingUser.getUsername(), tenantId);
            if (loginUser == null) {
                throw new RuntimeException("加载已存在用户信息失败");
            }
            return loginUser;
        }

        // 3. 未绑定且未匹配到已有用户，检查是否自动注册
        if (!Boolean.TRUE.equals(socialProperties.getAutoRegister())) {
            throw new RuntimeException("该账号未绑定，请先绑定账号");
        }

        // 4. 自动注册新用户
        log.info("三方登录自动注册: platform={}, uuid={}, nickname={}", platform, socialUuid, socialNickname);

        // 生成用户名（用platform + uuid的方式，避免过长）
        String username = platform.toLowerCase() + "_" + socialUuid;

        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(SysUser::getUsername, username);
        if (tenantId != null) {
            checkWrapper.eq(SysUser::getTenantId, tenantId);
        }
        checkWrapper.last("limit 1");
        SysUser newUser;
        SysUser sysUser = userMapper.selectOne(checkWrapper);
        if (sysUser != null) {
            newUser = sysUser;
        } else {
            // 创建用户
            newUser = new SysUser();
            newUser.setTenantId(tenantId);
            newUser.setUsername(username);
            newUser.setRealName(StrUtil.isNotBlank(socialNickname) ? socialNickname : "三方用户");
            newUser.setUserType(2);
            newUser.setEmail(socialEmail);
            if (StrUtil.isNotBlank(request.getPhone())) {
                newUser.setPhone(request.getPhone());
            }
            
            // 设置默认密码
            String initPassword = sysConfigService.selectConfigByKey("sys.user.initPassword");
            if (StrUtil.isBlank(initPassword)) {
                initPassword = "123456"; // 默认密码
            }
            newUser.setPassword(PasswordUtil.encrypt(initPassword));
            newUser.setUserStatus(1);
            newUser.setAvatar(socialAvatar);
            
            // 设置默认部门（TODO：需要根据实际业务设置）
            // newUser.setCreateDept(...);
            
            userMapper.insert(newUser);
            log.info("三方登录自动创建用户: userId={}, username={}", newUser.getId(), newUser.getUsername());
        }
        // 5. 绑定三方账号
        AuthUser au = new AuthUser();
        au.setUuid(socialUuid);
        au.setUsername(username);
        au.setNickname(socialNickname);
        au.setAvatar(socialAvatar);
        au.setEmail(socialEmail);
        socialUserService.bindSocialUser(newUser.getId(), au, platform, tenantId);

        // 6. 加载并返回LoginUser
        LoginUser loginUser = userLoadService.loadUserByUsername(newUser.getUsername(), tenantId);
        if (loginUser == null) {
            throw new RuntimeException("加载新用户信息失败");
        }

        log.info("三方登录自动注册成功: platform={}, uuid={}, userId={}", platform, socialUuid, newUser.getId());
        return loginUser;
    }

    @Override
    public String getAuthType() {
        return AuthType.OAUTH2.getCode();
    }

    @Override
    public boolean supports(LoginRequest request) {
        return AuthType.OAUTH2.getCode().equals(request.getAuthType())
                && StrUtil.isNotBlank(request.getSocialPlatform())
                && StrUtil.isNotBlank(request.getSocialUuid());
    }
}
