package com.park.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.park.sales.approval.ApprovalGateway;
import com.park.sales.approval.ApprovalGatewayFactory;
import com.park.sales.approval.ApprovalNodeDef;
import com.park.sales.common.BusinessException;
import com.park.sales.dto.ContractCreateRequest;
import com.park.sales.entity.ApprovalFlow;
import com.park.sales.entity.Contract;
import com.park.sales.entity.Subscription;
import com.park.sales.mapper.ContractMapper;
import com.park.sales.mapper.SubscriptionMapper;
import com.park.sales.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContractService {

    private static final Map<String, String> NODE_NAMES = new LinkedHashMap<>();

    static {
        NODE_NAMES.put("SALES_MANAGER", "销售经理审批");
        NODE_NAMES.put("FINANCE", "财务审批");
        NODE_NAMES.put("LEGAL", "法务审批");
        NODE_NAMES.put("GM", "总经理审批");
    }

    private final ContractMapper contractMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final ApprovalGatewayFactory gatewayFactory;

    @Value("${app.approval.chain:SALES_MANAGER,FINANCE,LEGAL}")
    private String approvalChain;

    public ContractService(ContractMapper contractMapper,
                           SubscriptionMapper subscriptionMapper,
                           ApprovalGatewayFactory gatewayFactory) {
        this.contractMapper = contractMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.gatewayFactory = gatewayFactory;
    }

    public IPage<Contract> page(long current, long size) {
        return contractMapper.selectPageWithJoin(new Page<>(current, size));
    }

    public Contract get(Long id) {
        Contract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        return contract;
    }

    @Transactional
    public Contract create(ContractCreateRequest req) {
        Subscription sub = subscriptionMapper.selectById(req.getSubscriptionId());
        if (sub == null) {
            throw new BusinessException("认购单不存在");
        }
        if (!"ACTIVE".equals(sub.getStatus())) {
            throw new BusinessException("该认购单状态不可生成合同");
        }

        BigDecimal discount = req.getDiscount() == null ? BigDecimal.ZERO : req.getDiscount();
        BigDecimal amount = sub.getTotalPrice().subtract(discount);

        Contract contract = new Contract();
        contract.setContractNo("HT" + System.currentTimeMillis());
        contract.setSubscriptionId(sub.getId());
        contract.setCustomerId(sub.getCustomerId());
        contract.setFactoryUnitId(sub.getFactoryUnitId());
        contract.setAmount(amount);
        contract.setDiscount(discount);
        contract.setPaymentMethod(req.getPaymentMethod() == null ? "FULL" : req.getPaymentMethod());
        contract.setStatus("DRAFT");
        contract.setSalesId(SecurityUtil.currentUserId());
        contract.setTerms(req.getTerms());
        contractMapper.insert(contract);

        sub.setStatus("CONTRACTED");
        subscriptionMapper.updateById(sub);
        return contract;
    }

    @Transactional
    public Contract submit(Long contractId) {
        Contract contract = get(contractId);
        if (!"DRAFT".equals(contract.getStatus()) && !"REJECTED".equals(contract.getStatus())) {
            throw new BusinessException("仅草稿/已驳回的合同可提交审批");
        }

        ApprovalGateway gateway = gatewayFactory.current();
        ApprovalFlow flow = gateway.start("CONTRACT", contract.getId(), buildChain());

        contract.setStatus("APPROVING");
        contract.setApprovalFlowId(flow.getId());
        contractMapper.updateById(contract);
        return contract;
    }

    private List<ApprovalNodeDef> buildChain() {
        List<ApprovalNodeDef> chain = new ArrayList<>();
        for (String role : approvalChain.split(",")) {
            String r = role.trim();
            if (r.isEmpty()) {
                continue;
            }
            chain.add(new ApprovalNodeDef(NODE_NAMES.getOrDefault(r, r + "审批"), r));
        }
        if (chain.isEmpty()) {
            throw new BusinessException("未配置审批链");
        }
        return chain;
    }
}
