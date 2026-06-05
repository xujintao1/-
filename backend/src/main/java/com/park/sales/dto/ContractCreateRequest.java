package com.park.sales.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContractCreateRequest {
    @NotNull(message = "请选择认购单")
    private Long subscriptionId;
    /** 优惠金额 */
    private BigDecimal discount;
    /** 付款方式: FULL/INSTALLMENT/MORTGAGE */
    private String paymentMethod;
    private String terms;
}
