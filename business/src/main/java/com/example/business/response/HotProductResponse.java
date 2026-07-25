package com.example.business.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class HotProductResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;
    private String productTitle;
    private String image;
    private long soldQuantity;
    private BigDecimal paidAmount;
}
