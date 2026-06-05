package com.park.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.park.sales.common.BusinessException;
import com.park.sales.dto.PaymentCreateRequest;
import com.park.sales.entity.Contract;
import com.park.sales.entity.PaymentRecord;
import com.park.sales.mapper.ContractMapper;
import com.park.sales.mapper.PaymentRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final ContractMapper contractMapper;

    public PaymentService(PaymentRecordMapper paymentRecordMapper, ContractMapper contractMapper) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.contractMapper = contractMapper;
    }

    public List<PaymentRecord> listByContract(Long contractId) {
        return paymentRecordMapper.selectList(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getContractId, contractId)
                .orderByDesc(PaymentRecord::getId));
    }

    public PaymentRecord create(PaymentCreateRequest req) {
        Contract contract = contractMapper.selectById(req.getContractId());
        if (contract == null) {
            throw new BusinessException("合同不存在");
        }
        PaymentRecord record = new PaymentRecord();
        record.setContractId(req.getContractId());
        record.setCustomerId(contract.getCustomerId());
        record.setPaymentType(req.getPaymentType() == null ? "DEPOSIT" : req.getPaymentType());
        record.setAmount(req.getAmount());
        record.setPaymentDate(req.getPaymentDate() == null ? LocalDate.now() : req.getPaymentDate());
        record.setRemark(req.getRemark());
        paymentRecordMapper.insert(record);
        return record;
    }
}
