package com.park.sales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_record")
public class PaymentRecord extends BaseEntity {
    private Long contractId;
    private Long customerId;
    /** 回款类型: DEPOSIT(定金)/DOWN_PAYMENT(首付)/INSTALLMENT(分期)/FINAL(尾款) */
    private String paymentType;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String remark;

    @TableField(exist = false)
    private String contractNo;
}
