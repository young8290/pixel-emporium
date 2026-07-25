package com.example.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.business.entity.Cart;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface CartMapper extends BaseMapper<Cart> {
    @Update("""
            update cart
               set deleted = id,
                   deleted_at = now(),
                   deleted_by = #{userId},
                   updated_at = now(),
                   updated_by = #{userId}
             where id = #{id}
               and user_id = #{userId}
               and deleted = 0
            """)
    int logicalDeleteOwned(@Param("userId") Long userId, @Param("id") Long id);

    @Update("""
            <script>
            update cart
               set deleted = id,
                   deleted_at = now(),
                   deleted_by = #{userId},
                   updated_at = now(),
                   updated_by = #{userId}
             where user_id = #{userId}
               and deleted = 0
               and id in
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    int logicalDeleteBatch(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    @Update("""
            update cart
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
