package com.park.sales.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统参数配置（key-value，按 configGroup 分组）。
 * 用于 OA 对接地址、回调地址、审批网关开关、合同审批链等可在「系统设置」中维护的参数。
 */
@Data
@TableName("sys_config")
public class SysConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 配置分组：oa_integration / workflow / system */
    private String configGroup;
    /** 配置键 */
    private String configKey;
    /** 配置值 */
    private String configValue;
    /** 值类型：string / boolean / number */
    private String valueType;
    /** 描述 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
