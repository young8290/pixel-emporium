package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("order_item")
public class OrderItem extends BaseEntity {
    private Long orderId;
    private Long productId;
    private Long productSkuId;
    private String productTitle;
    private String skuTitle;
    private String image;
    private BigDecimal price;
    private Integer quantity;
}
