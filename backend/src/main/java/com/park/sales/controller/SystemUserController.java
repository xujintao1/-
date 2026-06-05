package com.park.sales.controller;

import com.park.sales.common.BusinessException;
import com.park.sales.common.Result;
import com.park.sales.entity.User;
import com.park.sales.service.SystemUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统管理 - 用户管理。仅 ADMIN 可访问。
 *
 * <p>用户身份数据同步自 OA 系统（对齐 hr- 仓库 UserController）：列表展示 OA 用户并叠加本地授权，
 * 本系统只允许"分配角色 + 启用/禁用"，不允许直接新增/修改/删除用户或重置密码。
 */
@RestController
@RequestMapping("/system/users")
@PreAuthorize("hasRole('ADMIN')")
public class SystemUserController {

    private static final String OA_MANAGED = "用户数据同步自OA系统，请在OA系统中管理用户";

    private final SystemUserService userService;

    public SystemUserController(SystemUserService userService) {
        this.userService = userService;
    }

    /** 用户列表：OA 用户 + 本地授权叠加（OA 不可用时回退本地数据） */
    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long deptId) {
        return Result.ok(userService.listForManagement(keyword, deptId));
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Long id) {
        return Result.ok(userService.get(id));
    }

    /** 分配授权：保存角色 + 启用状态（按用户名 upsert 本地授权记录） */
    @PostMapping("/assign-role")
    public Result<Void> assignRole(@RequestBody Map<String, Object> body) {
        String username = asString(body.get("username"));
        String realName = asString(body.get("realName"));
        String phone = asString(body.get("phone"));
        String deptName = asString(body.get("deptName"));
        String roles = asString(body.get("roles"));
        Integer enabled = body.get("enabled") != null ? Integer.valueOf(asString(body.get("enabled"))) : null;
        userService.assignRole(username, realName, phone, deptName, roles, enabled);
        return Result.ok();
    }

    /** 新增用户 - 数据同步自 OA，不允许在本系统直接创建 */
    @PostMapping
    public Result<Void> create(@RequestBody User user) {
        throw new BusinessException(OA_MANAGED);
    }

    /** 修改用户 - 数据同步自 OA，不允许在本系统直接修改 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody User user) {
        throw new BusinessException(OA_MANAGED);
    }

    /** 删除用户 - 数据同步自 OA，不允许在本系统直接删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        throw new BusinessException(OA_MANAGED);
    }

    /** 重置密码 - 密码由 OA 统一管理，本系统不允许重置 */
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, String> body) {
        throw new BusinessException(OA_MANAGED);
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
