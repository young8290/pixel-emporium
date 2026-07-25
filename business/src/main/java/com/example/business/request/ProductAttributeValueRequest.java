package com.example.business.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductAttributeValueRequest {
    private Long id;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "规格属性ID不能为空")
    private Long attributeId;

    @NotBlank(message = "规格值不能为空")
    @Size(max = 128, message = "规格值不能超过128个字符")
    private String value;

    @Min(value = 0, message = "排序值不能小于0")
    private Integer sort = 99;
}
