package com.example.business.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductAttributeRequest {
    private Long id;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotBlank(message = "规格名称不能为空")
    @Size(max = 64, message = "规格名称不能超过64个字符")
    private String name;

    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort = 99;
}
