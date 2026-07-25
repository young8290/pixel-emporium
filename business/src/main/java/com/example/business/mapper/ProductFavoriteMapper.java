package com.example.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.business.entity.ProductFavorite;
import com.example.business.response.ProductResponse;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ProductFavoriteMapper extends BaseMapper<ProductFavorite> {
    @Select("""
            select count(1) > 0
              from product_favorite
             where user_id = #{userId}
               and product_id = #{productId}
               and deleted = 0
            """)
    boolean existsActive(@Param("userId") Long userId, @Param("productId") Long productId);

    @Select("""
            select p.id, p.title, p.description, p.image, p.on_sale,
                   p.rating, p.sold_count, p.review_count, p.price,
                   p.category_id, c.name as category_name, p.created_at
              from product_favorite f
              join product p on p.id = f.product_id and p.deleted = 0
              left join category c on c.id = p.category_id and c.deleted = 0
             where f.user_id = #{userId}
               and f.deleted = 0
             order by f.created_at desc, f.id desc
            """)
    Page<ProductResponse> selectProductPage(Page<ProductResponse> page,
                                            @Param("userId") Long userId);

    @Update("""
            update product_favorite
               set deleted = id,
                   deleted_at = now(),
                   deleted_by = #{userId},
                   updated_at = now(),
                   updated_by = #{userId}
             where user_id = #{userId}
               and product_id = #{productId}
               and deleted = 0
            """)
    int logicalDeleteActive(@Param("userId") Long userId, @Param("productId") Long productId);
}
