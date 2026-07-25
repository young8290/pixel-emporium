package com.example.business.request;

import com.example.common.request.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductReviewPublicListRequest extends PageRequest {
    @Schema(description = "排序方式", allowableValues = {"newest", "highest", "lowest"})
    private String sort = "newest";

    @Schema(description = "星级筛选", allowableValues = {"1", "2", "3", "4", "5"})
    private Integer rating;
}
