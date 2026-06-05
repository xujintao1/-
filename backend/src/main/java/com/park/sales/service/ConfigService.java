package com.park.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.park.sales.entity.SysConfig;
import com.park.sales.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统参数配置服务，参照 hr- 仓库 ConfigService 实现。
 */
@Service
public class ConfigService extends ServiceImpl<SysConfigMapper, SysConfig> {

    /** 获取所有配置，按分组聚合 */
    public Map<String, List<SysConfig>> getAllConfigs() {
        List<SysConfig> configs = list();
        return configs.stream().collect(Collectors.groupingBy(SysConfig::getConfigGroup));
    }

    /** 获取单个配置值 */
    public String getConfig(String key) {
        SysConfig config = getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key), false);
        return config != null ? config.getConfigValue() : null;
    }

    /** 获取配置值，带默认值 */
    public String getConfig(String key, String defaultValue) {
        String value = getConfig(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /** 获取布尔配置 */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getConfig(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value);
    }

    /** 保存或更新配置 */
    public void saveConfig(String group, String key, String value) {
        SysConfig config = getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key), false);
        if (config == null) {
            config = new SysConfig();
            config.setConfigGroup(group);
            config.setConfigKey(key);
            config.setConfigValue(value);
            save(config);
        } else {
            config.setConfigValue(value);
            if (group != null && !group.isEmpty()) {
                config.setConfigGroup(group);
            }
            updateById(config);
        }
    }

    /** 批量更新配置 */
    public void batchUpdateConfigs(List<Map<String, String>> updates) {
        for (Map<String, String> update : updates) {
            saveConfig(update.get("group"), update.get("key"), update.get("value"));
        }
    }
}
