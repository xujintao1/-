package com.park.sales.controller;

import com.park.sales.common.Result;
import com.park.sales.entity.SysConfig;
import com.park.sales.service.ConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统参数配置接口，参照 hr- 仓库 ConfigController。
 */
@RestController
@RequestMapping("/system/config")
@PreAuthorize("hasRole('ADMIN')")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    /** 获取所有配置（按分组） */
    @GetMapping
    public Result<Map<String, List<SysConfig>>> getAllConfigs() {
        return Result.ok(configService.getAllConfigs());
    }

    /** 获取单个配置 */
    @GetMapping("/{key}")
    public Result<String> getConfig(@PathVariable String key) {
        return Result.ok(configService.getConfig(key));
    }

    /** 批量更新配置 */
    @PutMapping("/batch")
    public Result<Void> batchUpdate(@RequestBody List<Map<String, String>> updates) {
        configService.batchUpdateConfigs(updates);
        return Result.ok();
    }
}
