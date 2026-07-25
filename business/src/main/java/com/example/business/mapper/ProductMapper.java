package com.example.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.business.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface ProductMapper extends BaseMapper<Product> {
    @Select("select * from product where id = #{id} and deleted = 0 for update")
    Product selectByIdForUpdate(@Param("id") Long id);

    @Update("""
            update product
               set rating = #{averageRating}, review_count = #{reviewCount}
             where id = #{productId} and deleted = 0
            """)
    int updateReviewAggregate(@Param("productId") Long productId,
                              @Param("averageRating") BigDecimal averageRating,
                              @Param("reviewCount") long reviewCount);

    @Update("""
            update product p
              join (
                    select oi.product_id, sum(oi.quantity) as quantity
                      from order_item oi
                     where oi.order_id = #{orderId} and oi.deleted = 0
                     group by oi.product_id
                   ) paid on paid.product_id = p.id
               set p.sold_count = coalesce(p.sold_count, 0) + paid.quantity
             where p.deleted = 0
            """)
    int incrementSoldCountByOrderId(@Param("orderId") Long orderId);
}
