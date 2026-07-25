package com.example.business.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartUpdateRequest {
    @NotNull(message = "购物车项ID不能为空")
    private Long id;

    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;

    private Boolean checked;
}
