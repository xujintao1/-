package com.park.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.park.sales.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
