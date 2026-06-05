package com.park.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.park.sales.common.BusinessException;
import com.park.sales.common.Result;
import com.park.sales.entity.SysDept;
import com.park.sales.mapper.SysDeptMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统管理 - 部门管理（树形）。对齐 hr- 仓 DeptController。
 */
@RestController
@RequestMapping("/system/dept")
@PreAuthorize("hasRole('ADMIN')")
public class SystemDeptController {

    private final SysDeptMapper deptMapper;

    public SystemDeptController(SysDeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    @GetMapping("/tree")
    public Result<List<SysDept>> tree() {
        return Result.ok(buildTree(listAll()));
    }

    @GetMapping("/list")
    public Result<List<SysDept>> list() {
        return Result.ok(listAll());
    }

    @GetMapping("/{id}")
    public Result<SysDept> getById(@PathVariable Long id) {
        return Result.ok(deptMapper.selectById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody SysDept dept) {
        if (dept.getDeptName() == null || dept.getDeptName().isBlank()) {
            throw new BusinessException("部门名称不能为空");
        }
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        if (dept.getSort() == null) {
            dept.setSort(0);
        }
        deptMapper.insert(dept);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysDept dept) {
        if (dept.getId() == null) {
            throw new BusinessException("缺少部门ID");
        }
        deptMapper.updateById(dept);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long childCount = deptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException("存在子部门，无法删除");
        }
        deptMapper.deleteById(id);
        return Result.ok();
    }

    private List<SysDept> listAll() {
        return deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort).orderByAsc(SysDept::getId));
    }

    private List<SysDept> buildTree(List<SysDept> all) {
        List<SysDept> roots = new ArrayList<>();
        for (SysDept dept : all) {
            Long parentId = dept.getParentId() == null ? 0L : dept.getParentId();
            if (parentId == 0L) {
                roots.add(dept);
            } else {
                for (SysDept candidate : all) {
                    if (candidate.getId().equals(parentId)) {
                        if (candidate.getChildren() == null) {
                            candidate.setChildren(new ArrayList<>());
                        }
                        candidate.getChildren().add(dept);
                        break;
                    }
                }
            }
        }
        return roots;
    }
}
