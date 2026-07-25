package com.example.business.controller.admin;

import com.example.business.request.OrderAdminListRequest;
import com.example.business.request.OrderShipRequest;
import com.example.business.response.OrderAdminDetailResponse;
import com.example.business.response.OrderAdminResponse;
import com.example.business.service.OrderService;
import com.example.business.annotation.OperationLog;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.response.PageResponse;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("admin/order")
@CustomRequireRole({"ADMIN", "SUPER_ADMIN"})
public class OrderAdminController {
    private final OrderService orderService;

    public OrderAdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "管理端订单列表")
    @GetMapping("/list")
    public R<PageResponse<OrderAdminResponse>> list(@Valid OrderAdminListRequest request) {
        return R.ok(orderService.adminList(request));
    }

    @Operation(summary = "管理端订单详情")
    @GetMapping("/{id}")
    public R<OrderAdminDetailResponse> detail(@PathVariable Long id) {
        return R.ok(orderService.adminDetail(id));
    }

    @Operation(summary = "订单发货")
    @OperationLog(action = "ORDER_SHIP", targetType = "ORDER", targetIdSpEL = "#id")
    @PutMapping("/{id}/ship")
    public R<Boolean> ship(@PathVariable Long id, @RequestBody @Valid OrderShipRequest request) {
        return R.ok(orderService.ship(id, request));
    }
}
