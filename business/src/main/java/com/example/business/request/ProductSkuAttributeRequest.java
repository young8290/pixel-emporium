package com.example.business.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductSkuAttributeRequest {
    @NotNull(message = "规格属性ID不能为空")
    private Long attributeId;

    @NotEmpty(message = "规格值不能为空")
    private List<Long> attributeValueIds;
}
