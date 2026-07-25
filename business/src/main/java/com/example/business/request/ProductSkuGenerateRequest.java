package com.example.business.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductSkuGenerateRequest {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Valid
    private List<ProductSkuAttributeRequest> selections;

    @NotNull(message = "默认价格不能为空")
    private BigDecimal defaultPrice;

    @NotNull(message = "默认库存不能为空")
    @PositiveOrZero(message = "默认库存不能小于0")
    private Integer defaultStock;
}
