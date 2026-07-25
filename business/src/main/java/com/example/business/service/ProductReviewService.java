package com.example.business.service;

import com.example.business.entity.ProductReview;
import com.example.business.request.ProductReviewCreateRequest;
import com.example.business.request.ProductReviewAdminListRequest;
import com.example.business.request.ProductReviewModerateRequest;
import com.example.business.request.ProductReviewPublicListRequest;
import com.example.business.request.ProductReviewUpdateRequest;
import com.example.business.response.ProductReviewResponse;
import com.example.business.response.ProductReviewAdminResponse;
import com.example.business.response.ProductReviewSummaryResponse;
import com.example.business.response.OrderReviewEligibilityResponse;
import com.example.common.response.PageResponse;

public interface ProductReviewService {
    PageResponse<ProductReviewResponse> publicList(Long productId,
                                                   ProductReviewPublicListRequest request);

    ProductReviewSummaryResponse summary(Long productId);

    OrderReviewEligibilityResponse orderEligibility(Long orderId);

    PageResponse<ProductReviewAdminResponse> adminList(ProductReviewAdminListRequest request);

    boolean hide(Long id, ProductReviewModerateRequest request);

    boolean restore(Long id);

    ProductReview create(ProductReviewCreateRequest request);

    ProductReview update(Long id, ProductReviewUpdateRequest request);

    boolean delete(Long id);
}
