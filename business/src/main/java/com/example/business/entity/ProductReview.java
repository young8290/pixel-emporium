package com.example.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.business.enums.ProductReviewStatus;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("product_review")
public class ProductReview extends BaseEntity {
    private Long productId;
    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private Integer rating;
    private String content;
    private ProductReviewStatus status;
    private String moderationReason;
    private LocalDateTime moderatedAt;
    private Long moderatedBy;
}
