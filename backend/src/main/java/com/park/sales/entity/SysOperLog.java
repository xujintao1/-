package com.park.sales.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志：记录管理端的增删改等关键操作，供「系统管理 - 操作日志」查询。
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 操作模块，如 合同管理 / 用户管理 */
    private String module;
    /** 操作类型：CREATE/UPDATE/DELETE/OTHER */
    private String operType;
    /** 操作描述 */
    private String title;
    /** 请求方法（类.方法） */
    private String method;
    /** 请求方式 GET/POST/PUT/DELETE */
    private String requestMethod;
    /** 请求 URI */
    private String requestUri;
    /** 操作人用户名 */
    private String operName;
    /** 请求参数（截断） */
    private String operParam;
    /** 1 成功 / 0 失败 */
    private Integer status;
    /** 失败错误信息 */
    private String errorMsg;
    /** 耗时（毫秒） */
    private Long costTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime operTime;
}
