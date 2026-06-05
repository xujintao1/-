package com.park.sales.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.park.sales.common.BusinessException;
import com.park.sales.common.Result;
import com.park.sales.entity.FactoryUnit;
import com.park.sales.mapper.FactoryUnitMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/factory-units")
public class FactoryUnitController {

    private final FactoryUnitMapper factoryUnitMapper;

    public FactoryUnitController(FactoryUnitMapper factoryUnitMapper) {
        this.factoryUnitMapper = factoryUnitMapper;
    }

    @GetMapping
    public Result<IPage<FactoryUnit>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(factoryUnitMapper.selectPageWithJoin(new Page<>(current, size), status, keyword));
    }

    @GetMapping("/{id}")
    public Result<FactoryUnit> get(@PathVariable Long id) {
        return Result.ok(factoryUnitMapper.selectById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SALES_MANAGER')")
    public Result<FactoryUnit> create(@RequestBody FactoryUnit unit) {
        computeTotalPrice(unit);
        if (unit.getStatus() == null) {
            unit.setStatus("ON_SALE");
        }
        factoryUnitMapper.insert(unit);
        return Result.ok(unit);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_MANAGER')")
    public Result<FactoryUnit> update(@PathVariable Long id, @RequestBody FactoryUnit unit) {
        FactoryUnit existing = factoryUnitMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("厂房单元不存在");
        }
        unit.setId(id);
        computeTotalPrice(unit);
        factoryUnitMapper.updateById(unit);
        return Result.ok(factoryUnitMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SALES_MANAGER')")
    public Result<Void> delete(@PathVariable Long id) {
        factoryUnitMapper.deleteById(id);
        return Result.ok();
    }

    private void computeTotalPrice(FactoryUnit unit) {
        if (unit.getArea() != null && unit.getUnitPrice() != null) {
            unit.setTotalPrice(unit.getArea().multiply(unit.getUnitPrice()).setScale(2, BigDecimal.ROUND_HALF_UP));
        }
    }
}
