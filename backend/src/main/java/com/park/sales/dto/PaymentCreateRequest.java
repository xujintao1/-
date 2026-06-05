package com.park.sales.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentCreateRequest {
    @NotNull(message = "请选择合同")
    private Long contractId;
    @NotNull(message = "请填写回款金额")
    private BigDecimal amount;
    /** DEPOSIT/DOWN_PAYMENT/INSTALLMENT/FINAL */
    private String paymentType;
    private LocalDate paymentDate;
    private String remark;
}
