package com.park.sales.controller;

import com.park.sales.common.Result;
import com.park.sales.dto.LoginResponse;
import com.park.sales.service.SsoService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SSO 单点登录控制器，参照 hr- 仓库 {@code com.hr.controller.SsoController}。
 * 对接外部 OA 系统实现统一身份认证。
 */
@RestController
@RequestMapping("/sso")
public class SsoController {

    private final SsoService ssoService;

    public SsoController(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    /** SSO 登录（通过 OA 系统认证账号密码） */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody Map<String, String> body) {
        return Result.ok(ssoService.ssoLogin(body.get("username"), body.get("password")));
    }

    /**
     * 自动 SSO 登录：优先使用请求体中的 token，其次使用 sso_token cookie。
     * OA 主动跳转或跨子域 cookie 携带 sso_token 时调用。
     */
    @PostMapping("/auto-login")
    public Result<LoginResponse> autoLogin(
            @CookieValue(name = "sso_token", required = false) String cookieToken,
            @RequestBody(required = false) Map<String, String> body) {
        String token = body != null ? body.get("token") : null;
        if (token == null || token.isEmpty()) {
            token = cookieToken;
        }
        if (token == null || token.isEmpty()) {
            return Result.fail(400, "未检测到 OA 登录状态");
        }
        return Result.ok(ssoService.autoLogin(token));
    }
}
