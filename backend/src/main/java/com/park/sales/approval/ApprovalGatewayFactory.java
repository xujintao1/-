package com.park.sales.approval;

import com.park.sales.service.ConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 选择审批网关实现：优先读取系统配置 {@code approval_gateway}，
 * 其次回落到 {@code app.approval.gateway}（默认 internal）。
 */
@Component
public class ApprovalGatewayFactory {

    private final List<ApprovalGateway> gateways;
    private final ConfigService configService;

    @Value("${app.approval.gateway:internal}")
    private String configuredGateway;

    public ApprovalGatewayFactory(List<ApprovalGateway> gateways, ConfigService configService) {
        this.gateways = gateways;
        this.configService = configService;
    }

    /** 当前生效的审批网关（系统配置优先） */
    public ApprovalGateway current() {
        String gateway = configService.getConfig("approval_gateway", configuredGateway);
        return byType(gateway);
    }

    /** 按类型获取审批网关（用于按流程已有 gateway 执行撤销/重提/删除） */
    public ApprovalGateway byType(String type) {
        String target = (type == null || type.isBlank()) ? configuredGateway : type;
        return gateways.stream()
                .filter(g -> g.getType().equalsIgnoreCase(target))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到审批网关实现: " + target));
    }
}
