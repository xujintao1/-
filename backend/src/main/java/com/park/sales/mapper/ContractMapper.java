package com.park.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.park.sales.entity.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

    @Select("""
            SELECT ct.*, c.name AS customerName, fu.unit_no AS unitNo
            FROM contract ct
            LEFT JOIN customer c ON c.id = ct.customer_id
            LEFT JOIN factory_unit fu ON fu.id = ct.factory_unit_id
            WHERE ct.deleted = 0
            ORDER BY ct.id DESC
            """)
    IPage<Contract> selectPageWithJoin(Page<Contract> page);
}
