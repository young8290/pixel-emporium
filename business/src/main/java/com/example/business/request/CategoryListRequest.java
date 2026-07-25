package com.example.business.request;

import com.example.common.request.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryListRequest extends PageRequest {
    @Schema(description = "分类名称关键词")
    private String keyword;

    @Schema(description = "父分类ID，0为顶级")
    private Long parentId;

    @Schema(description = "启用状态")
    private Integer enabled;
}
