package com.example.business.service;

import com.example.business.request.OrderCreateRequest;
import com.example.business.request.OrderAdminListRequest;
import com.example.business.request.OrderListRequest;
import com.example.business.request.OrderShipRequest;
import com.example.business.response.OrderDetailResponse;
import com.example.business.response.OrderAdminDetailResponse;
import com.example.business.response.OrderAdminResponse;
import com.example.business.response.OrderResponse;
import com.example.common.response.PageResponse;

import java.time.LocalDateTime;

public interface OrderService {
    String create(OrderCreateRequest request);

    PageResponse<OrderResponse> list(OrderListRequest request);

    PageResponse<OrderAdminResponse> adminList(OrderAdminListRequest request);

    OrderDetailResponse detail(Long id);

    OrderAdminDetailResponse adminDetail(Long id);

    boolean cancel(Long id);

    boolean confirm(Long id);

    boolean ship(Long id, OrderShipRequest request);

    boolean markPaid(Long id, LocalDateTime paidAt);

    int cancelTimeoutOrders();
}
