package com.example.business.controller;

import com.example.business.request.OrderCreateRequest;
import com.example.business.request.OrderListRequest;
import com.example.business.response.OrderDetailResponse;
import com.example.business.response.OrderResponse;
import com.example.business.service.OrderService;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("order")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public R<String> create(@RequestBody @Valid OrderCreateRequest request) {
        return R.ok(orderService.create(request));
    }

    @Operation(summary = "查询当前用户订单")
    @GetMapping("/list")
    public R<PageResponse<OrderResponse>> list(@Valid OrderListRequest request) {
        return R.ok(orderService.list(request));
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/{id}")
    public R<OrderDetailResponse> detail(@PathVariable Long id) {
        return R.ok(orderService.detail(id));
    }

    @Operation(summary = "取消待支付订单")
    @PutMapping("/{id}/cancel")
    public R<Boolean> cancel(@PathVariable Long id) {
        return R.ok(orderService.cancel(id));
    }

    @Operation(summary = "确认收货")
    @PutMapping("/{id}/confirm")
    public R<Boolean> confirm(@PathVariable Long id) {
        return R.ok(orderService.confirm(id));
    }
}
