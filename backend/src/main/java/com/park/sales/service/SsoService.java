package com.park.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.park.sales.common.BusinessException;
import com.park.sales.dto.LoginResponse;
import com.park.sales.entity.User;
import com.park.sales.mapper.UserMapper;
import com.park.sales.security.JwtUtil;
import com.park.sales.security.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * SSO 单点登录服务，参照 hr- 仓库 {@code com.hr.service.SsoService} 实现。
 *
 * <p>账号与密码由外部 OA 系统统一校验（OA 是身份认证中心，用户信息在 OA 维护）；
 * 本系统的 sys_user 表只负责<b>授权</b>：决定该 OA 用户能否进入本系统、拥有哪些角色。
 * 与 HR 一致——OA 校验通过但本地没有该用户时，提示“无权限访问系统，请联系管理员”。
 *
 * <p>是否启用 OA 单点登录由系统配置 {@code oa_sso_enabled} 控制：
 * 启用且配置了 OA 地址时走 OA 认证；否则回落到本地用户名/密码认证，
 * 保证系统在没有 OA 环境时仍可独立运行（与本仓库审批网关 internal/oa 的可切换设计一致）。
 */
@Service
public class SsoService {

    private static final Logger log = LoggerFactory.getLogger(SsoService.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OaApiClient oaApiClient;
    private final ConfigService configService;

    public SsoService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                      OaApiClient oaApiClient, ConfigService configService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.oaApiClient = oaApiClient;
        this.configService = configService;
    }

    /** 是否启用 OA 单点登录：配置开关打开且已配置 OA 地址 */
    public boolean ssoEnabled() {
        return configService.getBoolean("oa_sso_enabled", false) && oaApiClient.isConfigured();
    }

    /**
     * 登录入口：启用 OA SSO 时走 OA 认证，否则本地认证。
     */
    public LoginResponse login(String username, String password) {
        if (ssoEnabled()) {
            return ssoLogin(username, password);
        }
        return localLogin(username, password);
    }

    /**
     * 通过 OA 系统进行单点登录认证。
     * 与 HR 流程一致：OA 校验账号密码 -> 本地查授权用户 -> 校验状态 -> 签发本系统 JWT。
     */
    public LoginResponse ssoLogin(String username, String password) {
        JsonNode data = oaApiClient.ssoLogin(username, password);
        String oaUsername = data.has("username") ? data.get("username").asText() : username;

        User user = findByUsername(oaUsername);
        if (user == null) {
            throw new BusinessException(403, "无权限访问系统，请联系管理员添加用户权限");
        }
        ensureEnabled(user);
        log.info("[SSO] OA 单点登录成功: {}", oaUsername);
        return buildResponse(user, data);
    }

    /**
     * 通过 OA Token 自动登录（OA 主动跳转或跨域 cookie 携带 sso_token 时）。
     * 参照 hr- 仓库 SsoService.autoLogin。
     */
    public LoginResponse autoLogin(String oaToken) {
        if (!ssoEnabled()) {
            throw new BusinessException(400, "未启用 OA 单点登录");
        }
        JsonNode data = oaApiClient.validateToken(oaToken);
        if (data == null || !data.path("valid").asBoolean(false)) {
            throw new BusinessException(401, "OA 登录已过期，请重新登录");
        }
        String username = data.get("username").asText();
        User user = findByUsername(username);
        if (user == null) {
            throw new BusinessException(403, "无权限访问系统，请联系管理员添加用户权限");
        }
        ensureEnabled(user);
        log.info("[SSO] OA 自动登录成功: {}", username);
        return buildResponse(user, data);
    }

    /** 本地用户名/密码认证（未启用 OA SSO 时的回落方案） */
    private LoginResponse localLogin(String username, String password) {
        User user = findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        ensureEnabled(user);
        return buildResponse(user, null);
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    private void ensureEnabled(User user) {
        if (user.getEnabled() == null || user.getEnabled() != 1) {
            throw new BusinessException(400, "账号已被禁用，请联系管理员");
        }
    }

    /** 生成本系统 JWT 并组装登录响应；OA 返回的 realName 优先于本地 */
    private LoginResponse buildResponse(User user, JsonNode oaData) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LoginUser loginUser = new LoginUser(user);
        String realName = user.getRealName();
        if (oaData != null && oaData.has("realName") && !oaData.get("realName").isNull()) {
            realName = oaData.get("realName").asText();
        }
        return new LoginResponse(token, user.getId(), user.getUsername(), realName, loginUser.getRoleCodes());
    }
}
