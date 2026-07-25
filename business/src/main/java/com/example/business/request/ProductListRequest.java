package com.example.business.request;

import com.example.common.request.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductListRequest extends PageRequest {
    @Schema(description = "商品名称关键词")
    private String keyword;

    @Schema(description = "上架状态")
    private Boolean onSale;

    @Schema(description = "分类ID")
    private Long categoryId;
}
