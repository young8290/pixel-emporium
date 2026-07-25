package com.example.business.controller;

import com.example.business.request.CartAddRequest;
import com.example.business.request.CartUpdateRequest;
import com.example.business.response.CartViewResponse;
import com.example.business.service.CartService;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "查询购物车")
    @GetMapping
    public R<CartViewResponse> getCart() {
        return R.ok(cartService.getCart());
    }

    @Operation(summary = "加入购物车")
    @PostMapping("/add")
    public R<Boolean> add(@RequestBody @Valid CartAddRequest request) {
        return R.ok(cartService.add(request));
    }

    @Operation(summary = "更新购物车项")
    @PutMapping("/update")
    public R<Boolean> update(@RequestBody @Valid CartUpdateRequest request) {
        return R.ok(cartService.update(request));
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(cartService.delete(id));
    }

    @Operation(summary = "批量删除购物车项")
    @DeleteMapping("/batch")
    public R<Boolean> deleteBatch(@RequestBody List<Long> ids) {
        return R.ok(cartService.deleteBatch(ids));
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public R<Boolean> clear() {
        return R.ok(cartService.clear());
    }
}
