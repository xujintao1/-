package com.park.sales.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubscriptionCreateRequest {
    @NotNull(message = "请选择客户")
    private Long customerId;
    @NotNull(message = "请选择厂房单元")
    private Long factoryUnitId;
    @NotNull(message = "请填写定金")
    private BigDecimal deposit;
    private String remark;
}
