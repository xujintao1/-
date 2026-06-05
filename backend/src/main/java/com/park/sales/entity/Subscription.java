package com.park.sales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("subscription")
public class Subscription extends BaseEntity {
    /** 认购单号 */
    private String subscriptionNo;
    private Long customerId;
    private Long factoryUnitId;
    /** 定金(元) */
    private BigDecimal deposit;
    /** 成交总价(元) */
    private BigDecimal totalPrice;
    /** 状态: ACTIVE/CANCELLED/CONTRACTED */
    private String status;
    private Long salesId;
    private String remark;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String unitNo;
}
