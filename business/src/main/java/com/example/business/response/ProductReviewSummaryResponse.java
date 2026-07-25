package com.example.business.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class ProductReviewSummaryResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;
    private BigDecimal averageRating;
    private Long reviewCount;
    private Map<Integer, Long> distribution = new LinkedHashMap<>();
}
