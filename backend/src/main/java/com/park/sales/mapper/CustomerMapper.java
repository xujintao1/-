package com.park.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.park.sales.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
