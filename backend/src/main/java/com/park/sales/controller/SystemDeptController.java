package com.park.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.park.sales.common.BusinessException;
import com.park.sales.common.Result;
import com.park.sales.entity.SysDept;
import com.park.sales.mapper.SysDeptMapper;
import com.park.sales.service.OaApiClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 系统管理 - 部门管理。部门数据同步自 OA 系统（对齐 hr- 仓库 DeptController）。
 *
 * <p>部门以 OA 为数据源：树/列表优先从 OA 拉取，OA 未配置或调用失败时回退本地 sys_dept 数据。
 * 与 HR 一致，新增/编辑/删除一律拒绝，提示前往 OA 系统维护。
 */
@RestController
@RequestMapping("/system/dept")
@PreAuthorize("hasRole('ADMIN')")
public class SystemDeptController {

    private static final String OA_MANAGED = "部门数据同步自OA系统，请在OA系统中管理部门";

    private final SysDeptMapper deptMapper;
    private final OaApiClient oaApiClient;

    public SystemDeptController(SysDeptMapper deptMapper, OaApiClient oaApiClient) {
        this.deptMapper = deptMapper;
        this.oaApiClient = oaApiClient;
    }

    /** 获取部门树（从 OA 系统同步，OA 不可用时回退本地数据） */
    @GetMapping("/tree")
    public Result<?> tree() {
        List<Map<String, Object>> oaDeptTree = oaApiClient.getOaDeptTree();
        if (oaDeptTree != null && !oaDeptTree.isEmpty()) {
            return Result.ok(oaDeptTree);
        }
        return Result.ok(buildTree(listAll()));
    }

    /** 获取部门列表（从 OA 系统同步，OA 不可用时回退本地数据） */
    @GetMapping("/list")
    public Result<?> list() {
        List<Map<String, Object>> oaDeptTree = oaApiClient.getOaDeptTree();
        if (oaDeptTree != null && !oaDeptTree.isEmpty()) {
            List<Map<String, Object>> flat = new ArrayList<>();
            flattenDeptTree(oaDeptTree, flat);
            return Result.ok(flat);
        }
        return Result.ok(listAll());
    }

    @GetMapping("/{id}")
    public Result<SysDept> getById(@PathVariable Long id) {
        return Result.ok(deptMapper.selectById(id));
    }

    /** 创建部门 - 数据同步自 OA，不允许在本系统直接创建 */
    @PostMapping
    public Result<Void> create(@RequestBody SysDept dept) {
        throw new BusinessException(OA_MANAGED);
    }

    /** 更新部门 - 数据同步自 OA，不允许在本系统直接修改 */
    @PutMapping
    public Result<Void> update(@RequestBody SysDept dept) {
        throw new BusinessException(OA_MANAGED);
    }

    /** 删除部门 - 数据同步自 OA，不允许在本系统直接删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        throw new BusinessException(OA_MANAGED);
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

    @SuppressWarnings("unchecked")
    private void flattenDeptTree(List<Map<String, Object>> tree, List<Map<String, Object>> result) {
        for (Map<String, Object> node : tree) {
            result.add(node);
            Object children = node.get("children");
            if (children instanceof List) {
                flattenDeptTree((List<Map<String, Object>>) children, result);
            }
        }
    }
}
