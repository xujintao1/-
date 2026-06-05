package com.park.sales.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.park.sales.common.BusinessException;
import com.park.sales.dto.SubscriptionCreateRequest;
import com.park.sales.entity.Customer;
import com.park.sales.entity.FactoryUnit;
import com.park.sales.entity.Subscription;
import com.park.sales.mapper.CustomerMapper;
import com.park.sales.mapper.FactoryUnitMapper;
import com.park.sales.mapper.SubscriptionMapper;
import com.park.sales.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;
    private final FactoryUnitMapper factoryUnitMapper;
    private final CustomerMapper customerMapper;

    public SubscriptionService(SubscriptionMapper subscriptionMapper,
                               FactoryUnitMapper factoryUnitMapper,
                               CustomerMapper customerMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.factoryUnitMapper = factoryUnitMapper;
        this.customerMapper = customerMapper;
    }

    public IPage<Subscription> page(long current, long size) {
        return subscriptionMapper.selectPageWithJoin(new Page<>(current, size));
    }

    @Transactional
    public Subscription create(SubscriptionCreateRequest req) {
        Customer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        FactoryUnit unit = factoryUnitMapper.selectById(req.getFactoryUnitId());
        if (unit == null) {
            throw new BusinessException("厂房单元不存在");
        }
        if (!"ON_SALE".equals(unit.getStatus())) {
            throw new BusinessException("该厂房单元当前不可认购（状态：" + unit.getStatus() + "）");
        }

        Subscription sub = new Subscription();
        sub.setSubscriptionNo("RG" + System.currentTimeMillis());
        sub.setCustomerId(req.getCustomerId());
        sub.setFactoryUnitId(req.getFactoryUnitId());
        sub.setDeposit(req.getDeposit());
        sub.setTotalPrice(unit.getTotalPrice());
        sub.setStatus("ACTIVE");
        sub.setSalesId(SecurityUtil.currentUserId());
        sub.setRemark(req.getRemark());
        subscriptionMapper.insert(sub);

        unit.setStatus("SUBSCRIBED");
        factoryUnitMapper.updateById(unit);
        return sub;
    }

    @Transactional
    public void cancel(Long id) {
        Subscription sub = subscriptionMapper.selectById(id);
        if (sub == null) {
            throw new BusinessException("认购单不存在");
        }
        if (!"ACTIVE".equals(sub.getStatus())) {
            throw new BusinessException("仅生效中的认购单可取消");
        }
        sub.setStatus("CANCELLED");
        subscriptionMapper.updateById(sub);

        FactoryUnit unit = factoryUnitMapper.selectById(sub.getFactoryUnitId());
        if (unit != null && "SUBSCRIBED".equals(unit.getStatus())) {
            unit.setStatus("ON_SALE");
            factoryUnitMapper.updateById(unit);
        }
    }
}
