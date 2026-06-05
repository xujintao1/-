package com.park.sales.controller;

import com.park.sales.common.Result;
import com.park.sales.entity.User;
import com.park.sales.service.SystemUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统设置 - 用户管理。仅 ADMIN 可访问。
 */
@RestController
@RequestMapping("/system/users")
@PreAuthorize("hasRole('ADMIN')")
public class SystemUserController {

    private final SystemUserService userService;

    public SystemUserController(SystemUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<List<User>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(userService.list(keyword));
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Long id) {
        return Result.ok(userService.get(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody User user) {
        userService.create(user);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody User user) {
        userService.update(id, user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/enabled")
    public Result<Void> setEnabled(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object enabled = body.get("enabled");
        userService.setEnabled(id, Boolean.parseBoolean(String.valueOf(enabled)));
        return Result.ok();
    }

    @PostMapping("/{id}/reset-password")
    public Result<Map<String, String>> resetPassword(@PathVariable Long id,
                                                      @RequestBody(required = false) Map<String, String> body) {
        String newPassword = body == null ? null : body.get("password");
        String raw = userService.resetPassword(id, newPassword);
        return Result.ok(Map.of("password", raw));
    }
}
