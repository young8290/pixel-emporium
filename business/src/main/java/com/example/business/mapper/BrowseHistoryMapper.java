package com.example.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.business.entity.BrowseHistory;
import com.example.business.response.BrowseHistoryResponse;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface BrowseHistoryMapper extends BaseMapper<BrowseHistory> {
    @Select("""
            select *
              from user_browse_history
             where user_id = #{userId}
               and product_id = #{productId}
               and deleted = 0
             limit 1
            """)
    BrowseHistory selectActive(@Param("userId") Long userId, @Param("productId") Long productId);

    @Update("""
            update user_browse_history
               set view_count = view_count + 1,
                   last_viewed_at = now(),
                   updated_at = now(),
                   updated_by = #{userId}
             where user_id = #{userId}
               and product_id = #{productId}
               and deleted = 0
            """)
    int incrementActive(@Param("userId") Long userId, @Param("productId") Long productId);

    @Select("""
            select p.id, h.id as history_id, p.title, p.description, p.image, p.on_sale,
                   p.rating, p.sold_count, p.review_count, p.price,
                   p.category_id, c.name as category_name,
                   h.view_count, h.last_viewed_at
              from user_browse_history h
              join product p on p.id = h.product_id and p.deleted = 0
              left join category c on c.id = p.category_id and c.deleted = 0
             where h.user_id = #{userId}
               and h.deleted = 0
             order by h.last_viewed_at desc, h.id desc
            """)
    Page<BrowseHistoryResponse> selectProductPage(Page<BrowseHistoryResponse> page,
                                                  @Param("userId") Long userId);

    @Update("""
            update user_browse_history
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

    @Update("""
            update user_browse_history
               set deleted = id,
                   deleted_at = now(),
                   deleted_by = #{userId},
                   updated_at = now(),
                   updated_by = #{userId}
             where user_id = #{userId}
               and deleted = 0
            """)
    int logicalDeleteAll(@Param("userId") Long userId);
}
