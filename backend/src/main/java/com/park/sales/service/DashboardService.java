package com.park.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.park.sales.entity.Contract;
import com.park.sales.entity.FactoryUnit;
import com.park.sales.entity.PaymentRecord;
import com.park.sales.mapper.ContractMapper;
import com.park.sales.mapper.CustomerMapper;
import com.park.sales.mapper.FactoryUnitMapper;
import com.park.sales.mapper.PaymentRecordMapper;
import com.park.sales.mapper.SubscriptionMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final FactoryUnitMapper factoryUnitMapper;
    private final CustomerMapper customerMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ContractMapper contractMapper;
    private final PaymentRecordMapper paymentRecordMapper;

    public DashboardService(FactoryUnitMapper factoryUnitMapper,
                            CustomerMapper customerMapper,
                            SubscriptionMapper subscriptionMapper,
                            ContractMapper contractMapper,
                            PaymentRecordMapper paymentRecordMapper) {
        this.factoryUnitMapper = factoryUnitMapper;
        this.customerMapper = customerMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.contractMapper = contractMapper;
        this.paymentRecordMapper = paymentRecordMapper;
    }

    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();

        long totalUnits = factoryUnitMapper.selectCount(null);
        long onSale = countUnitByStatus("ON_SALE");
        long subscribed = countUnitByStatus("SUBSCRIBED");
        long signed = countUnitByStatus("SIGNED");
        long sold = countUnitByStatus("SOLD");

        result.put("totalUnits", totalUnits);
        result.put("onSaleUnits", onSale);
        result.put("subscribedUnits", subscribed);
        result.put("signedUnits", signed);
        result.put("soldUnits", sold);
        result.put("customerCount", customerMapper.selectCount(null));
        result.put("subscriptionCount", subscriptionMapper.selectCount(null));
        result.put("contractCount", contractMapper.selectCount(null));

        // 已生效合同金额
        BigDecimal effectiveAmount = contractMapper.selectList(new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getStatus, "EFFECTIVE")).stream()
                .map(Contract::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("effectiveContractAmount", effectiveAmount);

        // 累计回款
        BigDecimal totalReceived = paymentRecordMapper.selectList(null).stream()
                .map(PaymentRecord::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("totalReceived", totalReceived);

        // 房源状态分布（前端图表用）
        List<Map<String, Object>> unitStatus = List.of(
                statusItem("在售", onSale),
                statusItem("已认购", subscribed),
                statusItem("已签约", signed),
                statusItem("已售", sold));
        result.put("unitStatusDistribution", unitStatus);

        return result;
    }

    private long countUnitByStatus(String status) {
        return factoryUnitMapper.selectCount(new LambdaQueryWrapper<FactoryUnit>()
                .eq(FactoryUnit::getStatus, status));
    }

    private Map<String, Object> statusItem(String name, long value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }
}
