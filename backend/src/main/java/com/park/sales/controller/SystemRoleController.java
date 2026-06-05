package com.park.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.park.sales.common.BusinessException;
import com.park.sales.common.Result;
import com.park.sales.entity.Role;
import com.park.sales.mapper.RoleMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统设置 - 角色管理。仅 ADMIN 可访问。
 */
@RestController
@RequestMapping("/system/roles")
@PreAuthorize("hasRole('ADMIN')")
public class SystemRoleController {

    private final RoleMapper roleMapper;

    public SystemRoleController(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @GetMapping
    public Result<List<Role>> list() {
        return Result.ok(roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId)));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Role role) {
        if (role.getCode() == null || role.getCode().isBlank()) {
            throw new BusinessException("角色编码不能为空");
        }
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<Role>().eq(Role::getCode, role.getCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException("角色编码已存在");
        }
        roleMapper.insert(role);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Role role) {
        Role existing = roleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("角色不存在");
        }
        existing.setName(role.getName());
        existing.setDescription(role.getDescription());
        roleMapper.updateById(existing);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleMapper.deleteById(id);
        return Result.ok();
    }
}
