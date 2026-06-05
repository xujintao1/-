package com.park.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.park.sales.common.BusinessException;
import com.park.sales.common.Result;
import com.park.sales.entity.Customer;
import com.park.sales.mapper.CustomerMapper;
import com.park.sales.security.SecurityUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerMapper customerMapper;

    public CustomerController(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    @GetMapping
    public Result<IPage<Customer>> page(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Customer::getName, keyword).or().like(Customer::getContactPerson, keyword);
        }
        wrapper.orderByDesc(Customer::getId);
        return Result.ok(customerMapper.selectPage(new Page<>(current, size), wrapper));
    }

    @GetMapping("/all")
    public Result<List<Customer>> all() {
        return Result.ok(customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                .orderByDesc(Customer::getId)));
    }

    @PostMapping
    public Result<Customer> create(@RequestBody Customer customer) {
        customer.setOwnerId(SecurityUtil.currentUserId());
        customerMapper.insert(customer);
        return Result.ok(customer);
    }

    @PutMapping("/{id}")
    public Result<Customer> update(@PathVariable Long id, @RequestBody Customer customer) {
        Customer existing = customerMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("客户不存在");
        }
        customer.setId(id);
        customerMapper.updateById(customer);
        return Result.ok(customerMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerMapper.deleteById(id);
        return Result.ok();
    }
}
