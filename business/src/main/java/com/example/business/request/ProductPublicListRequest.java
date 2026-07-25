package com.example.business.request;

import com.example.common.request.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductPublicListRequest extends PageRequest {
    @Schema(description = "商品名称关键词")
    private String keyword;

    @Schema(description = "分类ID")
    private Long categoryId;

    @DecimalMin(value = "0.00", message = "最低价不能小于0")
    @Schema(description = "SKU最低成交价下限")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.00", message = "最高价不能小于0")
    @Schema(description = "SKU最低成交价上限")
    private BigDecimal maxPrice;

    @Schema(description = "是否只查看有库存商品")
    private Boolean inStock;

    @Schema(description = "排序方式", allowableValues = {"default", "priceAsc", "priceDesc", "sales", "newest", "rating"})
    private String sort = "default";
}
