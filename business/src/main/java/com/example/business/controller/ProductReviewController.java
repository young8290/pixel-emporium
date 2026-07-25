package com.example.business.controller;

import com.example.business.entity.ProductReview;
import com.example.business.request.ProductReviewCreateRequest;
import com.example.business.request.ProductReviewUpdateRequest;
import com.example.business.response.OrderReviewEligibilityResponse;
import com.example.business.response.ProductReviewResponse;
import com.example.business.service.ProductReviewService;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("review")
public class ProductReviewController {
    private final ProductReviewService reviewService;

    public ProductReviewController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "创建商品评价")
    @PostMapping
    public R<ProductReviewResponse> create(@RequestBody @Valid ProductReviewCreateRequest request) {
        return R.ok(toResponse(reviewService.create(request)));
    }

    @Operation(summary = "修改商品评价")
    @PutMapping("/{id}")
    public R<ProductReviewResponse> update(@PathVariable Long id,
                                           @RequestBody @Valid ProductReviewUpdateRequest request) {
        return R.ok(toResponse(reviewService.update(id, request)));
    }

    @Operation(summary = "删除商品评价")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(reviewService.delete(id));
    }

    @Operation(summary = "查询订单评价资格")
    @GetMapping("/order/{orderId}")
    public R<OrderReviewEligibilityResponse> orderEligibility(@PathVariable Long orderId) {
        return R.ok(reviewService.orderEligibility(orderId));
    }

    private ProductReviewResponse toResponse(ProductReview review) {
        if (review == null) {
            return null;
        }
        ProductReviewResponse response = new ProductReviewResponse();
        response.setId(review.getId());
        response.setProductId(review.getProductId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setReviewerLabel("已购用户");
        response.setVerifiedPurchase(true);
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }
}
