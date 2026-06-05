package com.park.sales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract")
public class Contract extends BaseEntity {
    /** 合同编号 */
    private String contractNo;
    private Long subscriptionId;
    private Long customerId;
    private Long factoryUnitId;
    /** 合同金额(元) */
    private BigDecimal amount;
    /** 优惠金额(元) */
    private BigDecimal discount;
    /** 付款方式: FULL(全款)/INSTALLMENT(分期)/MORTGAGE(按揭) */
    private String paymentMethod;
    /** 状态: DRAFT/APPROVING/APPROVED/REJECTED/EFFECTIVE */
    private String status;
    /** 关联审批流id */
    private Long approvalFlowId;
    private Long salesId;
    private String terms;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String unitNo;
}
