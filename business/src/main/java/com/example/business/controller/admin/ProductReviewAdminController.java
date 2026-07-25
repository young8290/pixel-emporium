package com.example.business.controller.admin;

import com.example.business.annotation.OperationLog;
import com.example.business.request.ProductReviewAdminListRequest;
import com.example.business.request.ProductReviewModerateRequest;
import com.example.business.response.ProductReviewAdminResponse;
import com.example.business.service.ProductReviewService;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin/review")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class ProductReviewAdminController {
    private final ProductReviewService reviewService;

    public ProductReviewAdminController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "管理端商品评价列表")
    @GetMapping("/list")
    public R<PageResponse<ProductReviewAdminResponse>> list(
            @Valid ProductReviewAdminListRequest request) {
        return R.ok(reviewService.adminList(request));
    }

    @Operation(summary = "隐藏商品评价")
    @PutMapping("/{id}/hide")
    @OperationLog(action = "REVIEW_HIDE", targetType = "PRODUCT_REVIEW", targetIdSpEL = "#id")
    public R<Boolean> hide(@PathVariable Long id,
                           @RequestBody @Valid ProductReviewModerateRequest request) {
        return R.ok(reviewService.hide(id, request));
    }

    @Operation(summary = "恢复商品评价")
    @PutMapping("/{id}/restore")
    @OperationLog(action = "REVIEW_RESTORE", targetType = "PRODUCT_REVIEW", targetIdSpEL = "#id")
    public R<Boolean> restore(@PathVariable Long id) {
        return R.ok(reviewService.restore(id));
    }
}
