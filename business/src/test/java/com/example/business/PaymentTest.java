package com.example.business;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.example.business.entity.Order;
import com.example.business.entity.Payment;
import com.example.business.enums.OrderStatus;
import com.example.business.enums.PaymentStatus;
import com.example.business.mapper.OrderMapper;
import com.example.business.mapper.PaymentMapper;
import com.example.business.response.PaymentResponse;
import com.example.business.service.PaymentService;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("mysql-integration")
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "alibaba.cloud.oss.access-key=test-access-key",
        "alibaba.cloud.oss.secret-key=test-secret-key",
        "order.timeout-scan-interval-ms=3600000"
})
class PaymentTest {
    @Autowired private PaymentService paymentService;
    @Autowired private PaymentMapper paymentMapper;
    @Autowired private OrderMapper orderMapper;
    @Autowired private IdentifierGenerator identifierGenerator;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<Long> orderIds = new ArrayList<>();

    @AfterEach
    void cleanOnlyRowsCreatedByThisTest() {
        UserContext.remove();
        for (Long orderId : orderIds) {
            jdbcTemplate.update("delete from payment where order_id = ?", orderId);
            jdbcTemplate.update("delete from order_item where order_id = ?", orderId);
            jdbcTemplate.update("delete from `order` where id = ?", orderId);
        }
        orderIds.clear();
    }

    @Test
    void flywayV9AndRepeatedCreatePersistOneServerPricedPayment() {
        assertEquals(1, jdbcTemplate.queryForObject("""
                select count(*)
                  from information_schema.tables
                 where table_schema = database()
                   and table_name = 'payment'
                """, Integer.class));
        assertEquals(3, jdbcTemplate.queryForObject("""
                select count(distinct index_name)
                  from information_schema.statistics
                 where table_schema = database()
                   and table_name = 'payment'
                   and index_name in ('uk_payment_pay_no', 'uk_payment_order', 'idx_order')
                """, Integer.class));
        Order order = createOrder(nextId(), OrderStatus.PENDING_PAYMENT, "12.00", null);

        PaymentResponse first = paymentService.create(order.getOrderNo());
        PaymentResponse second = paymentService.create(order.getOrderNo());

        assertEquals(first.getPayNo(), second.getPayNo());
        assertEquals(0, new BigDecimal("12.00").compareTo(first.getAmount()));
        assertEquals("MOCK", first.getChannel());
        assertEquals(PaymentStatus.PENDING, first.getStatus());
        assertTrue(first.isMockPayment());
        assertTrue(first.isConfirmable());
        assertEquals(1L, paymentMapper.selectCount(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, order.getId())));
    }

    @Test
    void amountMismatchLeavesPaymentAndOrderPending() {
        Order order = createOrder(nextId(), OrderStatus.PENDING_PAYMENT, "12.00", null);
        PaymentResponse created = paymentService.create(order.getOrderNo());

        assertThrows(CustomConflictException.class,
                () -> paymentService.callback(
                        created.getPayNo(), new BigDecimal("11.99")));

        assertEquals(PaymentStatus.PENDING,
                paymentMapper.selectByOrderId(order.getId()).getStatus());
        assertEquals(OrderStatus.PENDING_PAYMENT,
                orderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    void successfulAndRepeatedCallbackAdvanceExactlyOnce() {
        Order order = createOrder(nextId(), OrderStatus.PENDING_PAYMENT, "12.00", null);
        PaymentResponse created = paymentService.create(order.getOrderNo());

        assertTrue(paymentService.callback(
                created.getPayNo(), new BigDecimal("12.0")));
        Payment firstPayment = paymentMapper.selectByOrderId(order.getId());
        Order firstOrder = orderMapper.selectById(order.getId());
        assertEquals(PaymentStatus.SUCCESS, firstPayment.getStatus());
        assertEquals(OrderStatus.PAID, firstOrder.getStatus());
        assertNotNull(firstPayment.getPaidAt());
        assertNotNull(firstOrder.getPaidAt());
        LocalDateTime paymentPaidAt = firstPayment.getPaidAt();
        LocalDateTime orderPaidAt = firstOrder.getPaidAt();

        assertTrue(paymentService.callback(
                created.getPayNo(), new BigDecimal("12.00")));

        Payment repeatedPayment = paymentMapper.selectByOrderId(order.getId());
        Order repeatedOrder = orderMapper.selectById(order.getId());
        assertEquals(paymentPaidAt, repeatedPayment.getPaidAt());
        assertEquals(orderPaidAt, repeatedOrder.getPaidAt());
        assertEquals(1L, paymentMapper.selectCount(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, order.getId())));
    }

    @Test
    void canceledOrderRejectsCallbackAndKeepsPaymentPending() {
        Order order = createOrder(nextId(), OrderStatus.PENDING_PAYMENT, "12.00", null);
        PaymentResponse created = paymentService.create(order.getOrderNo());
        order.setStatus(OrderStatus.CANCELED);
        order.setCanceledAt(LocalDateTime.of(2026, 7, 23, 11, 0));
        assertEquals(1, orderMapper.updateById(order));

        assertThrows(CustomConflictException.class,
                () -> paymentService.callback(
                        created.getPayNo(), new BigDecimal("12.00")));

        assertEquals(PaymentStatus.PENDING,
                paymentMapper.selectByOrderId(order.getId()).getStatus());
        assertEquals(OrderStatus.CANCELED,
                orderMapper.selectById(order.getId()).getStatus());
    }

    @Test
    void alreadyPaidOrderRepairsPendingPaymentFromOrderTimestamp() {
        LocalDateTime paidAt = LocalDateTime.of(2026, 7, 23, 10, 30);
        Order order = createOrder(nextId(), OrderStatus.PENDING_PAYMENT, "12.00", null);
        PaymentResponse created = paymentService.create(order.getOrderNo());
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(paidAt);
        assertEquals(1, orderMapper.updateById(order));

        assertTrue(paymentService.callback(
                created.getPayNo(), new BigDecimal("12.00")));

        Payment repaired = paymentMapper.selectByOrderId(order.getId());
        assertEquals(PaymentStatus.SUCCESS, repaired.getStatus());
        assertEquals(paidAt, repaired.getPaidAt());
        assertEquals(paidAt, orderMapper.selectById(order.getId()).getPaidAt());
    }

    @Test
    void foreignUserCannotCreateCallbackOrQueryPayment() {
        Long ownerId = nextId();
        Order order = createOrder(ownerId, OrderStatus.PENDING_PAYMENT, "12.00", null);
        PaymentResponse created = paymentService.create(order.getOrderNo());
        UserContext.set(nextId());

        assertThrows(CustomForbiddenException.class,
                () -> paymentService.create(order.getOrderNo()));
        assertThrows(CustomForbiddenException.class,
                () -> paymentService.callback(
                        created.getPayNo(), new BigDecimal("12.00")));
        assertThrows(CustomForbiddenException.class,
                () -> paymentService.status(order.getOrderNo()));

        assertEquals(PaymentStatus.PENDING,
                paymentMapper.selectByOrderId(order.getId()).getStatus());
        assertEquals(OrderStatus.PENDING_PAYMENT,
                orderMapper.selectById(order.getId()).getStatus());
    }

    private Order createOrder(Long userId,
                              OrderStatus status,
                              String amount,
                              LocalDateTime paidAt) {
        Order order = new Order();
        order.setId(nextId());
        order.setOrderNo("PAYTEST" + order.getId());
        order.setUserId(userId);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal(amount));
        order.setPayAmount(new BigDecimal(amount));
        order.setFreightAmount(new BigDecimal("0.00"));
        order.setReceiverName("张三");
        order.setReceiverPhone("13800138000");
        order.setReceiverProvince("广东省");
        order.setReceiverCity("深圳市");
        order.setReceiverDistrict("南山区");
        order.setReceiverDetail("科技园1号");
        order.setPaidAt(paidAt);
        assertEquals(1, orderMapper.insert(order));
        orderIds.add(order.getId());
        UserContext.set(userId);
        return order;
    }

    private Long nextId() {
        return identifierGenerator.nextId(this).longValue();
    }
}
