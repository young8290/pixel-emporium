package com.example.business.request;

import com.example.business.enums.ProductReviewStatus;
import com.example.common.request.PageRequest;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProductReviewAdminListRequest extends PageRequest {
    private Long productId;
    private Long userId;
    private Integer rating;
    private ProductReviewStatus status;
    private String keyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAtStart;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAtEnd;
}
