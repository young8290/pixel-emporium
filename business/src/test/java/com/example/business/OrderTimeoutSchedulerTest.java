package com.example.business;

import com.example.business.BusinessApplication;
import com.example.business.scheduler.OrderTimeoutScheduler;
import com.example.business.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderTimeoutSchedulerTest {
    @Test
    void schedulerDelegatesOneBoundedServiceScan() {
        OrderService service = mock(OrderService.class);
        OrderTimeoutScheduler scheduler = new OrderTimeoutScheduler(service);

        scheduler.run();

        verify(service).cancelTimeoutOrders();
    }

    @Test
    void applicationEnablesSchedulingAndIntervalIsConfigurable() throws Exception {
        assertNotNull(BusinessApplication.class.getAnnotation(EnableScheduling.class));
        Method run = OrderTimeoutScheduler.class.getMethod("run");
        Scheduled scheduled = run.getAnnotation(Scheduled.class);
        assertNotNull(scheduled);
        assertEquals("${order.timeout-scan-interval-ms:60000}", scheduled.fixedDelayString());
    }
}
