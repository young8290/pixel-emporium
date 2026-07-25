package com.example.business.scheduler;

import com.example.business.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutScheduler {
    private final OrderService orderService;

    public OrderTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${order.timeout-scan-interval-ms:60000}")
    public void run() {
        orderService.cancelTimeoutOrders();
    }
}
