package com.example.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.business.entity.Payment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PaymentMapper extends BaseMapper<Payment> {
    @Select("""
            select *
              from payment
             where order_id = #{orderId}
               and deleted = 0
            """)
    Payment selectByOrderId(@Param("orderId") Long orderId);

    @Select("""
            select *
              from payment
             where order_id = #{orderId}
               and deleted = 0
             for update
            """)
    Payment selectByOrderIdForUpdate(@Param("orderId") Long orderId);

    @Select("""
            select *
              from payment
             where pay_no = #{payNo}
               and deleted = 0
            """)
    Payment selectByPayNo(@Param("payNo") String payNo);

    @Select("""
            select *
              from payment
             where trade_no = #{tradeNo}
               and deleted = 0
            """)
    Payment selectByTradeNo(@Param("tradeNo") String tradeNo);

    @Select("""
            select *
              from payment
             where id = #{id}
               and deleted = 0
             for update
            """)
    Payment selectByIdForUpdate(@Param("id") Long id);
}
