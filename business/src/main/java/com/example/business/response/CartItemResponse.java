package com.example.business.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CartItemResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long productSkuId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    private String productTitle;
    private String productImage;
    private String skuTitle;
    private BigDecimal price;
    private Integer stock;
    private Integer quantity;
    private Boolean checked;
    private Boolean invalid;
}
