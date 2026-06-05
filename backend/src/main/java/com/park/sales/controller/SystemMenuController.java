package com.park.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.park.sales.common.BusinessException;
import com.park.sales.common.Result;
import com.park.sales.entity.SysMenu;
import com.park.sales.mapper.SysMenuMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统管理 - 菜单管理（树形）。对齐 hr- 仓 MenuController。
 */
@RestController
@RequestMapping("/system/menu")
@PreAuthorize("hasRole('ADMIN')")
public class SystemMenuController {

    private final SysMenuMapper menuMapper;

    public SystemMenuController(SysMenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @GetMapping("/list")
    public Result<List<SysMenu>> list() {
        return Result.ok(menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort).orderByAsc(SysMenu::getId)));
    }

    @GetMapping("/{id}")
    public Result<SysMenu> getById(@PathVariable Long id) {
        return Result.ok(menuMapper.selectById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody SysMenu menu) {
        if (menu.getMenuName() == null || menu.getMenuName().isBlank()) {
            throw new BusinessException("菜单名称不能为空");
        }
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getMenuType() == null) {
            menu.setMenuType("M");
        }
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        if (menu.getSort() == null) {
            menu.setSort(0);
        }
        menuMapper.insert(menu);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysMenu menu) {
        if (menu.getId() == null) {
            throw new BusinessException("缺少菜单ID");
        }
        menuMapper.updateById(menu);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long childCount = menuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
        return Result.ok();
    }
}
