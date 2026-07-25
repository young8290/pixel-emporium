package com.example.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.business.entity.Order;
import com.example.business.model.OrderAddressSnapshot;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderMapper extends BaseMapper<Order> {
    @Select("""
            select id, user_id, recipient, phone, province, city, district, detail
              from address
             where id = #{id}
               and deleted = 0
            """)
    OrderAddressSnapshot selectAddressById(@Param("id") Long id);

    @Select("""
            select *
              from `order`
             where id = #{id}
               and deleted = 0
             for update
            """)
    Order selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            select id
              from `order`
             where status = 'PENDING_PAYMENT'
               and created_at < #{deadline}
               and deleted = 0
             order by created_at asc, id asc
             limit #{limit}
            """)
    List<Long> selectTimeoutIds(@Param("deadline") LocalDateTime deadline,
                                @Param("limit") int limit);
}
