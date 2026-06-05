package com.park.sales.controller;

import com.park.sales.common.Result;
import com.park.sales.dto.LoginRequest;
import com.park.sales.dto.LoginResponse;
import com.park.sales.security.LoginUser;
import com.park.sales.security.SecurityUtil;
import com.park.sales.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
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
