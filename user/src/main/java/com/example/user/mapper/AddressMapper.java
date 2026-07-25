package com.example.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.user.entity.Address;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AddressMapper extends BaseMapper<Address> {
    @Select("select id from user where id = #{userId} for update")
    Long lockUserById(@Param("userId") Long userId);
}
