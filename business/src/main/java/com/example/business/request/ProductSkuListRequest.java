package com.example.business.request;

import com.example.common.request.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSkuListRequest extends PageRequest {
    @Schema(description = "SKU名称关键词")
    private String keyword;

    @Schema(description = "商品ID")
    private Long productId;
}
