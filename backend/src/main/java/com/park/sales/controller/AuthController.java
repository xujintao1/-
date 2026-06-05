package com.park.sales.controller;

import com.park.sales.common.Result;
import com.park.sales.dto.LoginRequest;
import com.park.sales.dto.LoginResponse;
import com.park.sales.security.LoginUser;
import com.park.sales.security.SecurityUtil;
import com.park.sales.service.SsoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SsoService ssoService;

    public AuthController(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    /**
     * 登录：启用 OA 单点登录时由 OA 校验账号密码，否则本地认证（见 {@link SsoService}）。
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(ssoService.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        LoginUser user = SecurityUtil.currentUser();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("userId", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getUser().getRealName());
        info.put("roles", user.getRoleCodes());
        return Result.ok(info);
    }
}
