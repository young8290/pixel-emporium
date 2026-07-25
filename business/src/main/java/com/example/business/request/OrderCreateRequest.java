package com.example.business.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderCreateRequest {
    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;

    @NotBlank(message = "下单来源不能为空")
    @Pattern(regexp = "^(cart|buyNow)$", message = "下单来源只能是cart或buyNow")
    private String source;

    private Long skuId;

    @Positive(message = "数量必须大于0")
    private Integer quantity;

    private List<Long> cartItemIds;

    @Size(max = 255, message = "订单备注不能超过255个字符")
    private String remark;
}
