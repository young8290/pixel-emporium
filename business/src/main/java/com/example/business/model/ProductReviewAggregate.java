package com.example.business.model;

import java.math.BigDecimal;

public record ProductReviewAggregate(Long count, BigDecimal averageRating,
                                     Long star1, Long star2, Long star3,
                                     Long star4, Long star5) {
    public ProductReviewAggregate(Long count, BigDecimal averageRating) {
        this(count, averageRating, 0L, 0L, 0L, 0L, 0L);
    }
}
