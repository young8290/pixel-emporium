package com.example.business.controller;

import com.example.business.response.ProductResponse;
import com.example.business.service.ProductFavoriteService;
import com.example.common.request.PageRequest;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("favorite")
public class ProductFavoriteController {
    private final ProductFavoriteService favoriteService;

    public ProductFavoriteController(ProductFavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Operation(summary = "查询我的商品收藏")
    @GetMapping("/list")
    public R<PageResponse<ProductResponse>> list(@Valid PageRequest request) {
        return R.ok(favoriteService.list(request));
    }

    @Operation(summary = "查询商品收藏状态")
    @GetMapping("/{productId}/status")
    public R<Boolean> status(@PathVariable Long productId) {
        return R.ok(favoriteService.isFavorite(productId));
    }

    @Operation(summary = "收藏商品")
    @PostMapping("/{productId}")
    public R<Boolean> add(@PathVariable Long productId) {
        return R.ok(favoriteService.add(productId));
    }

    @Operation(summary = "取消收藏商品")
    @DeleteMapping("/{productId}")
    public R<Boolean> remove(@PathVariable Long productId) {
        return R.ok(favoriteService.remove(productId));
    }
}
