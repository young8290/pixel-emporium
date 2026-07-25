package com.example.business;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.example.business.entity.Order;
import com.example.business.entity.Payment;
import com.example.business.enums.OrderStatus;
import com.example.business.enums.PaymentStatus;
import com.example.business.mapper.OrderMapper;
import com.example.business.mapper.PaymentMapper;
import com.example.business.response.PaymentResponse;
import com.example.business.service.OrderService;
import com.example.business.service.impl.PaymentServiceImpl;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomForbiddenException;
import com.example.common.exception.CustomValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class PaymentServiceTest {
    @Mock private PaymentMapper paymentMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderService orderService;
    @Mock private IdentifierGenerator identifierGenerator;

    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(
                paymentMapper, orderMapper, orderService, identifierGenerator);
        UserContext.set(11L);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void createLocksOrderThenPaymentAndPersistsServerAmount() {
        Order order = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(order);
        when(paymentMapper.selectByOrderIdForUpdate(501L)).thenReturn(null);
        when(identifierGenerator.nextId(any())).thenReturn(7001L);
        when(paymentMapper.insert(any(Payment.class))).thenReturn(1);

        PaymentResponse response = service.create(order.getOrderNo());

        InOrder locks = inOrder(orderMapper, paymentMapper);
        locks.verify(orderMapper).selectOne(any(Wrapper.class));
        locks.verify(orderMapper).selectByIdForUpdate(501L);
        locks.verify(paymentMapper).selectByOrderIdForUpdate(501L);
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper).insert(saved.capture());
        assertEquals(7001L, saved.getValue().getId());
        assertTrue(saved.getValue().getPayNo().matches("\\d{8}7001"));
        assertEquals(501L, saved.getValue().getOrderId());
        assertEquals(order.getOrderNo(), saved.getValue().getOrderNo());
        assertEquals(new BigDecimal("12.00"), saved.getValue().getAmount());
        assertEquals("MOCK", saved.getValue().getChannel());
        assertEquals(PaymentStatus.PENDING, saved.getValue().getStatus());
        assertEquals(saved.getValue().getPayNo(), response.getPayNo());
        assertEquals(OrderStatus.PENDING_PAYMENT, response.getOrderStatus());
        assertTrue(response.isMockPayment());
        assertTrue(response.isConfirmable());
    }

    @Test
    void createReusesPendingAndRejectsFinishedPayments() {
        Order order = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        Payment pending = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        Payment success = payment(7001L, 501L, "PAY1", PaymentStatus.SUCCESS, "12.00");
        Payment failed = payment(7001L, 501L, "PAY1", PaymentStatus.FAILED, "12.00");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(order);
        when(paymentMapper.selectByOrderIdForUpdate(501L))
                .thenReturn(pending, success, failed);

        assertEquals("PAY1", service.create(order.getOrderNo()).getPayNo());
        assertThrows(CustomConflictException.class,
                () -> service.create(order.getOrderNo()));
        assertThrows(CustomConflictException.class,
                () -> service.create(order.getOrderNo()));
        verify(paymentMapper, never()).insert(any(Payment.class));
    }

    @Test
    void createRejectsMissingLoginBlankAndMissingOrder() {
        UserContext.remove();
        assertThrows(CustomValidationException.class,
                () -> service.create("ORDER501"));
        UserContext.set(11L);
        assertThrows(CustomValidationException.class,
                () -> service.create(" "));
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        assertThrows(NoSuchElementException.class,
                () -> service.create("MISSING"));
    }

    @Test
    void createRejectsForeignOrderBeforeReadingPayment() {
        Order foreign = order(501L, 12L, OrderStatus.PENDING_PAYMENT, "12.00");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(foreign);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(foreign);

        assertThrows(CustomForbiddenException.class,
                () -> service.create(foreign.getOrderNo()));

        verify(paymentMapper, never()).selectByOrderIdForUpdate(any());
    }

    @Test
    void createRejectsEveryNonPendingOrderState() {
        Order observed = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(observed);
        when(orderMapper.selectByIdForUpdate(501L))
                .thenReturn(order(501L, 11L, OrderStatus.PAID, "12.00"))
                .thenReturn(order(501L, 11L, OrderStatus.SHIPPED, "12.00"))
                .thenReturn(order(501L, 11L, OrderStatus.COMPLETED, "12.00"))
                .thenReturn(order(501L, 11L, OrderStatus.CANCELED, "12.00"));

        for (int i = 0; i < 4; i++) {
            assertThrows(CustomConflictException.class,
                    () -> service.create(observed.getOrderNo()));
        }
        verify(paymentMapper, never()).selectByOrderIdForUpdate(any());
    }

    @Test
    void createRejectsCorruptAmountsAndPersistenceFailures() {
        Order observed = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        Order nullAmount = order(501L, 11L, OrderStatus.PENDING_PAYMENT, null);
        Order negativeAmount = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "-0.01");
        Order valid = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(observed);
        when(orderMapper.selectByIdForUpdate(501L))
                .thenReturn(nullAmount, negativeAmount, valid, valid);
        when(paymentMapper.selectByOrderIdForUpdate(501L)).thenReturn(null);
        when(identifierGenerator.nextId(any())).thenReturn(null, 7001L);
        when(paymentMapper.insert(any(Payment.class))).thenReturn(0);

        assertThrows(CustomBusinessException.class,
                () -> service.create(observed.getOrderNo()));
        assertThrows(CustomBusinessException.class,
                () -> service.create(observed.getOrderNo()));
        assertThrows(CustomBusinessException.class,
                () -> service.create(observed.getOrderNo()));
        assertThrows(CustomBusinessException.class,
                () -> service.create(observed.getOrderNo()));
    }

    @Test
    void statusReturnsOwnedPaymentAndComputesConfirmableFromBothStates() {
        Order pendingOrder = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        Payment pending = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(pendingOrder);
        when(paymentMapper.selectByOrderId(501L)).thenReturn(pending);

        PaymentResponse pendingResponse = service.status(pendingOrder.getOrderNo());

        assertEquals("PAY1", pendingResponse.getPayNo());
        assertEquals(PaymentStatus.PENDING, pendingResponse.getStatus());
        assertTrue(pendingResponse.isConfirmable());
        assertTrue(pendingResponse.isMockPayment());
        verify(orderMapper, never()).selectByIdForUpdate(any());

        pendingOrder.setStatus(OrderStatus.PAID);
        pending.setStatus(PaymentStatus.SUCCESS);
        pending.setPaidAt(LocalDateTime.of(2026, 7, 23, 12, 0));
        PaymentResponse successResponse = service.status(pendingOrder.getOrderNo());
        assertEquals(OrderStatus.PAID, successResponse.getOrderStatus());
        assertEquals(PaymentStatus.SUCCESS, successResponse.getStatus());
        assertFalse(successResponse.isConfirmable());
    }

    @Test
    void statusRejectsMissingForeignAndMissingPayment() {
        when(orderMapper.selectOne(any(Wrapper.class)))
                .thenReturn(null)
                .thenReturn(order(501L, 12L, OrderStatus.PENDING_PAYMENT, "12.00"))
                .thenReturn(order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00"));

        assertThrows(NoSuchElementException.class,
                () -> service.status("MISSING"));
        assertThrows(CustomForbiddenException.class,
                () -> service.status("ORDER501"));
        assertThrows(NoSuchElementException.class,
                () -> service.status("ORDER501"));
    }

    @Test
    void callbackRejectsMissingLoginBlankNullAndNegativeInput() {
        UserContext.remove();
        assertThrows(CustomValidationException.class,
                () -> service.callback("PAY1", new BigDecimal("12.00")));
        UserContext.set(11L);
        assertThrows(CustomValidationException.class,
                () -> service.callback(" ", new BigDecimal("12.00")));
        assertThrows(CustomValidationException.class,
                () -> service.callback("PAY1", null));
        assertThrows(CustomValidationException.class,
                () -> service.callback("PAY1", new BigDecimal("-0.01")));
        verify(paymentMapper, never()).selectByPayNo(any());
    }

    @Test
    void callbackRejectsMissingObservedOrderAndLockedPayment() {
        Payment observed = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        Order order = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        when(paymentMapper.selectByPayNo("PAY1"))
                .thenReturn(null, observed, observed);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(null, order);
        when(paymentMapper.selectByIdForUpdate(7001L)).thenReturn(null);

        assertThrows(NoSuchElementException.class,
                () -> service.callback("PAY1", new BigDecimal("12.00")));
        assertThrows(NoSuchElementException.class,
                () -> service.callback("PAY1", new BigDecimal("12.00")));
        assertThrows(NoSuchElementException.class,
                () -> service.callback("PAY1", new BigDecimal("12.00")));
    }

    @Test
    void callbackLocksOrderThenPaymentAndMarksBothSuccessful() {
        Payment observed = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        Order order = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        when(paymentMapper.selectByPayNo("PAY1")).thenReturn(observed);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(order);
        when(paymentMapper.selectByIdForUpdate(7001L)).thenReturn(observed);
        when(paymentMapper.updateById(observed)).thenReturn(1);
        when(orderService.markPaid(eq(501L), any(LocalDateTime.class))).thenReturn(true);

        assertTrue(service.callback("PAY1", new BigDecimal("12.0")));

        InOrder locks = inOrder(paymentMapper, orderMapper);
        locks.verify(paymentMapper).selectByPayNo("PAY1");
        locks.verify(orderMapper).selectByIdForUpdate(501L);
        locks.verify(paymentMapper).selectByIdForUpdate(7001L);
        InOrder writes = inOrder(paymentMapper, orderService);
        writes.verify(paymentMapper).updateById(observed);
        writes.verify(orderService).markPaid(501L, observed.getPaidAt());
        assertEquals(PaymentStatus.SUCCESS, observed.getStatus());
        assertTrue(observed.getPaidAt() != null);
    }

    @Test
    void callbackReturnsSuccessfulPaymentIdempotentlyWithoutCheckingNewAmount() {
        LocalDateTime firstPaidAt = LocalDateTime.of(2026, 7, 23, 12, 0);
        Payment success = payment(7001L, 501L, "PAY1", PaymentStatus.SUCCESS, "12.00");
        success.setPaidAt(firstPaidAt);
        Order shipped = order(501L, 11L, OrderStatus.SHIPPED, "12.00");
        when(paymentMapper.selectByPayNo("PAY1")).thenReturn(success);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(shipped);
        when(paymentMapper.selectByIdForUpdate(7001L)).thenReturn(success);

        assertTrue(service.callback("PAY1", new BigDecimal("0.01")));

        assertEquals(firstPaidAt, success.getPaidAt());
        verify(paymentMapper, never()).updateById(any(Payment.class));
        verify(orderService, never()).markPaid(any(), any());
    }

    @Test
    void callbackChecksOwnershipBeforeSuccessfulIdempotence() {
        Payment success = payment(7001L, 501L, "PAY1", PaymentStatus.SUCCESS, "12.00");
        Order foreign = order(501L, 12L, OrderStatus.PAID, "12.00");
        when(paymentMapper.selectByPayNo("PAY1")).thenReturn(success);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(foreign);

        assertThrows(CustomForbiddenException.class,
                () -> service.callback("PAY1", new BigDecimal("12.00")));

        verify(paymentMapper, never()).selectByIdForUpdate(any());
        verify(paymentMapper, never()).updateById(any(Payment.class));
    }

    @Test
    void callbackRejectsFailedAndBothKindsOfAmountMismatch() {
        Order order = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        Payment failed = payment(7001L, 501L, "PAY1", PaymentStatus.FAILED, "12.00");
        Payment pending = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        Payment wrongSnapshot = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "11.00");
        when(paymentMapper.selectByPayNo("PAY1")).thenReturn(failed, pending, wrongSnapshot);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(order);
        when(paymentMapper.selectByIdForUpdate(7001L))
                .thenReturn(failed, pending, wrongSnapshot);

        assertThrows(CustomConflictException.class,
                () -> service.callback("PAY1", new BigDecimal("12.00")));
        assertThrows(CustomConflictException.class,
                () -> service.callback("PAY1", new BigDecimal("11.00")));
        assertThrows(CustomConflictException.class,
                () -> service.callback("PAY1", new BigDecimal("12.00")));
        verify(paymentMapper, never()).updateById(any(Payment.class));
        verify(orderService, never()).markPaid(any(), any());
    }

    @Test
    void callbackRejectsAlipayPaymentRecords() {
        Payment alipay = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        alipay.setChannel("ALIPAY");
        Order order = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        when(paymentMapper.selectByPayNo("PAY1")).thenReturn(alipay);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(order);
        when(paymentMapper.selectByIdForUpdate(7001L)).thenReturn(alipay);

        assertThrows(CustomConflictException.class,
                () -> service.callback("PAY1", new BigDecimal("12.00")));
        verify(paymentMapper, never()).updateById(any(Payment.class));
    }

    @Test
    void callbackRepairsPendingPaymentForAlreadyPaidOrder() {
        LocalDateTime orderPaidAt = LocalDateTime.of(2026, 7, 23, 11, 30);
        Order paid = order(501L, 11L, OrderStatus.PAID, "12.00");
        paid.setPaidAt(orderPaidAt);
        Payment pending = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        when(paymentMapper.selectByPayNo("PAY1")).thenReturn(pending);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(paid);
        when(paymentMapper.selectByIdForUpdate(7001L)).thenReturn(pending);
        when(paymentMapper.updateById(pending)).thenReturn(1);

        assertTrue(service.callback("PAY1", new BigDecimal("12.00")));

        assertEquals(PaymentStatus.SUCCESS, pending.getStatus());
        assertEquals(orderPaidAt, pending.getPaidAt());
        verify(orderService, never()).markPaid(any(), any());
    }

    @Test
    void callbackUsesCurrentTimeWhenPaidOrderLostItsTimestamp() {
        Order paid = order(501L, 11L, OrderStatus.PAID, "12.00");
        Payment pending = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        when(paymentMapper.selectByPayNo("PAY1")).thenReturn(pending);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(paid);
        when(paymentMapper.selectByIdForUpdate(7001L)).thenReturn(pending);
        when(paymentMapper.updateById(pending)).thenReturn(1);

        assertTrue(service.callback("PAY1", new BigDecimal("12.00")));

        assertTrue(pending.getPaidAt() != null);
        verify(orderService, never()).markPaid(any(), any());
    }

    @Test
    void callbackRejectsCanceledShippedAndCompletedOrders() {
        Payment first = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        Payment second = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        Payment third = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        when(paymentMapper.selectByPayNo("PAY1")).thenReturn(first, second, third);
        when(orderMapper.selectByIdForUpdate(501L))
                .thenReturn(order(501L, 11L, OrderStatus.CANCELED, "12.00"))
                .thenReturn(order(501L, 11L, OrderStatus.SHIPPED, "12.00"))
                .thenReturn(order(501L, 11L, OrderStatus.COMPLETED, "12.00"));
        when(paymentMapper.selectByIdForUpdate(7001L)).thenReturn(first, second, third);

        for (int i = 0; i < 3; i++) {
            assertThrows(CustomConflictException.class,
                    () -> service.callback("PAY1", new BigDecimal("12.00")));
        }
        verify(paymentMapper, never()).updateById(any(Payment.class));
        verify(orderService, never()).markPaid(any(), any());
    }

    @Test
    void callbackPropagatesPaymentAndOrderWriteFailures() {
        Order order = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        Payment first = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        Payment second = payment(7001L, 501L, "PAY1", PaymentStatus.PENDING, "12.00");
        when(paymentMapper.selectByPayNo("PAY1")).thenReturn(first, second);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(order);
        when(paymentMapper.selectByIdForUpdate(7001L)).thenReturn(first, second);
        when(paymentMapper.updateById(any(Payment.class))).thenReturn(0, 1);
        when(orderService.markPaid(eq(501L), any(LocalDateTime.class))).thenReturn(false);

        assertThrows(CustomBusinessException.class,
                () -> service.callback("PAY1", new BigDecimal("12.00")));
        assertThrows(CustomBusinessException.class,
                () -> service.callback("PAY1", new BigDecimal("12.00")));
    }

    private Order order(Long id, Long userId, OrderStatus status, String amount) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo("ORDER" + id);
        order.setUserId(userId);
        order.setStatus(status);
        order.setPayAmount(amount == null ? null : new BigDecimal(amount));
        return order;
    }

    private Payment payment(Long id,
                            Long orderId,
                            String payNo,
                            PaymentStatus status,
                            String amount) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setOrderId(orderId);
        payment.setOrderNo("ORDER" + orderId);
        payment.setPayNo(payNo);
        payment.setAmount(new BigDecimal(amount));
        payment.setChannel("MOCK");
        payment.setStatus(status);
        return payment;
    }
}
