package com.park.sales.approval;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按配置 {@code app.approval.gateway} 选择审批网关实现。
 */
@Component
public class ApprovalGatewayFactory {

    private final List<ApprovalGateway> gateways;

    @Value("${app.approval.gateway:internal}")
    private String configuredGateway;

    public ApprovalGatewayFactory(List<ApprovalGateway> gateways) {
        this.gateways = gateways;
    }

    public ApprovalGateway current() {
        return gateways.stream()
                .filter(g -> g.getType().equalsIgnoreCase(configuredGateway))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到审批网关实现: " + configuredGateway));
    }
}
