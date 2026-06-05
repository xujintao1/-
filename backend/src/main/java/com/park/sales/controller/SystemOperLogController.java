package com.park.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.park.sales.common.Result;
import com.park.sales.entity.SysOperLog;
import com.park.sales.mapper.SysOperLogMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统管理 - 操作日志查询。
 */
@RestController
@RequestMapping("/system/oper-log")
@PreAuthorize("hasRole('ADMIN')")
public class SystemOperLogController {

    private final SysOperLogMapper operLogMapper;

    public SystemOperLogController(SysOperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    @GetMapping("/page")
    public Result<Page<SysOperLog>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operName) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<SysOperLog>()
                .like(module != null && !module.isBlank(), SysOperLog::getModule, module)
                .like(operName != null && !operName.isBlank(), SysOperLog::getOperName, operName)
                .orderByDesc(SysOperLog::getId);
        return Result.ok(operLogMapper.selectPage(new Page<>(current, size), wrapper));
    }

    @DeleteMapping("/clear")
    public Result<Void> clear() {
        operLogMapper.delete(new LambdaQueryWrapper<>());
        return Result.ok();
    }
}
