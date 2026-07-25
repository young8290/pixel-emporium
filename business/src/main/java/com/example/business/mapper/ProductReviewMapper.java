package com.example.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.business.entity.ProductReview;
import com.example.business.model.ProductReviewAggregate;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ProductReviewMapper extends BaseMapper<ProductReview> {
    @Select("""
            select *
              from product_review
             where id = #{id}
               and user_id = #{userId}
               and deleted = 0
            """)
    ProductReview selectOwnedActiveById(@Param("id") Long id,
                                        @Param("userId") Long userId);

    @Update("""
            update product_review
               set rating = #{rating}, content = #{content},
                   updated_at = now(), updated_by = #{userId}
             where id = #{id}
               and user_id = #{userId}
               and status = 'PUBLISHED'
               and deleted = 0
            """)
    int updatePublishedByOwner(@Param("id") Long id,
                               @Param("userId") Long userId,
                               @Param("rating") Integer rating,
                               @Param("content") String content);

    @Update("""
            update product_review
               set deleted = id, deleted_at = now(), deleted_by = #{userId}
             where id = #{id}
               and user_id = #{userId}
               and deleted = 0
            """)
    int softDeleteByOwner(@Param("id") Long id, @Param("userId") Long userId);

    @Update("""
            update product_review
               set status = 'HIDDEN', moderation_reason = #{reason},
                   moderated_at = now(), moderated_by = #{adminId},
                   updated_at = now(), updated_by = #{adminId}
             where id = #{id}
               and status = 'PUBLISHED'
               and deleted = 0
            """)
    int hidePublished(@Param("id") Long id,
                      @Param("adminId") Long adminId,
                      @Param("reason") String reason);

    @Update("""
            update product_review
               set status = 'PUBLISHED', moderation_reason = null,
                   moderated_at = now(), moderated_by = #{adminId},
                   updated_at = now(), updated_by = #{adminId}
             where id = #{id}
               and status = 'HIDDEN'
               and deleted = 0
            """)
    int restoreHidden(@Param("id") Long id, @Param("adminId") Long adminId);

    @Select("""
            select count(*) as count,
                   coalesce(round(avg(rating), 2), 0.00) as average_rating,
                   sum(case when rating = 1 then 1 else 0 end) as star1,
                   sum(case when rating = 2 then 1 else 0 end) as star2,
                   sum(case when rating = 3 then 1 else 0 end) as star3,
                   sum(case when rating = 4 then 1 else 0 end) as star4,
                   sum(case when rating = 5 then 1 else 0 end) as star5
              from product_review
             where product_id = #{productId}
               and status = 'PUBLISHED'
               and deleted = 0
            """)
    ProductReviewAggregate selectAggregate(@Param("productId") Long productId);
}
