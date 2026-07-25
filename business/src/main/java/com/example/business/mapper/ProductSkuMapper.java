package com.example.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.business.entity.ProductSku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    @Update("""
            update product_sku
               set stock = stock - #{qty}
             where id = #{id}
               and stock >= #{qty}
               and deleted = 0
            """)
    int deductStock(@Param("id") Long id, @Param("qty") Integer qty);

    @Update("""
            update product_sku
               set stock = stock + #{qty}
             where id = #{id}
               and deleted = 0
            """)
    int restoreStock(@Param("id") Long id, @Param("qty") Integer qty);
}
