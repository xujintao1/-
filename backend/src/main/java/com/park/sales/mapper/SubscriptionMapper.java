package com.park.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.park.sales.entity.Subscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SubscriptionMapper extends BaseMapper<Subscription> {

    @Select("""
            SELECT s.*, c.name AS customerName, fu.unit_no AS unitNo
            FROM subscription s
            LEFT JOIN customer c ON c.id = s.customer_id
            LEFT JOIN factory_unit fu ON fu.id = s.factory_unit_id
            WHERE s.deleted = 0
            ORDER BY s.id DESC
            """)
    IPage<Subscription> selectPageWithJoin(Page<Subscription> page);
}
