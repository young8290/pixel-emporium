package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("product_favorite")
public class ProductFavorite extends BaseEntity {
    private Long userId;
    private Long productId;
}
