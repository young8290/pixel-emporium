package com.example.business.controller;

import com.example.business.request.ProductPublicListRequest;
import com.example.business.request.ProductReviewPublicListRequest;
import com.example.business.response.ProductDetailResponse;
import com.example.business.response.ProductReviewResponse;
import com.example.business.response.ProductReviewSummaryResponse;
import com.example.business.response.ProductResponse;
import com.example.business.service.ProductReviewService;
import com.example.business.service.ProductService;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("product")
public class ProductController {
    private final ProductService productService;
    private final ProductReviewService productReviewService;

    public ProductController(ProductService productService, ProductReviewService productReviewService) {
        this.productService = productService;
        this.productReviewService = productReviewService;
    }

    @Operation(summary = "公开商品列表")
    @GetMapping("/list")
    public R<PageResponse<ProductResponse>> list(@Valid ProductPublicListRequest request) {
        return R.ok(productService.getPublicProductPage(request));
    }

    @Operation(summary = "商品搜索建议")
    @GetMapping("/suggestions")
    public R<List<String>> suggestions(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "10") Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit == null ? 10 : limit, 10));
        return R.ok(productService.getSearchSuggestions(keyword.trim(), safeLimit));
    }

    @Operation(summary = "公开商品详情")
    @GetMapping("/{id}")
    public R<ProductDetailResponse> detail(@PathVariable Long id) {
        return R.ok(productService.getPublicProductDetail(id));
    }

    @Operation(summary = "商品规则推荐")
    @GetMapping("/{id}/recommendations")
    public R<List<ProductResponse>> recommendations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "8") Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit == null ? 8 : limit, 20));
        return R.ok(productService.getRecommendations(id, safeLimit));
    }

    @Operation(summary = "公开商品评价")
    @GetMapping("/{id}/reviews")
    public R<PageResponse<ProductReviewResponse>> reviews(
            @PathVariable Long id,
            @Valid ProductReviewPublicListRequest request) {
        return R.ok(productReviewService.publicList(id, request));
    }

    @Operation(summary = "公开商品评分汇总")
    @GetMapping("/{id}/review-summary")
    public R<ProductReviewSummaryResponse> reviewSummary(@PathVariable Long id) {
        return R.ok(productReviewService.summary(id));
    }
}
