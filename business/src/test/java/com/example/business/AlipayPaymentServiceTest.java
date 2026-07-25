package com.example.business;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.example.business.client.AlipayGateway;
import com.example.business.client.AlipayPagePayCommand;
import com.example.business.client.AlipayTradeQueryResult;
import com.example.business.config.AlipayProperties;
import com.example.business.entity.Order;
import com.example.business.entity.Payment;
import com.example.business.enums.OrderStatus;
import com.example.business.enums.PaymentStatus;
import com.example.business.mapper.OrderMapper;
import com.example.business.mapper.PaymentMapper;
import com.example.business.response.AlipayPaymentResponse;
import com.example.business.response.PaymentResponse;
import com.example.business.service.OrderService;
import com.example.business.service.impl.AlipayPaymentServiceImpl;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class AlipayPaymentServiceTest {
    @Mock private PaymentMapper paymentMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderService orderService;
    @Mock private IdentifierGenerator identifierGenerator;
    @Mock private AlipayGateway gateway;

    private AlipayPaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        AlipayProperties properties = new AlipayProperties();
        properties.setEnabled(true);
        properties.setAppId("sandbox-app");
        properties.setSellerId("sandbox-seller");
        service = new AlipayPaymentServiceImpl(paymentMapper, orderMapper, orderService,
                identifierGenerator, gateway, properties);
        UserContext.set(11L);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void createsPagePaymentFromOwnedPendingOrderAndServerAmount() {
        Order order = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(order);
        when(paymentMapper.selectByOrderIdForUpdate(501L)).thenReturn(null);
        when(identifierGenerator.nextId(any())).thenReturn(7001L);
        when(paymentMapper.insert(any(Payment.class))).thenReturn(1);
        when(gateway.pagePay(any())).thenReturn("<form id=alipay></form>");

        AlipayPaymentResponse response = service.create(order.getOrderNo());

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentMapper).insert(saved.capture());
        assertEquals("ALIPAY", saved.getValue().getChannel());
        assertEquals(new BigDecimal("12.00"), saved.getValue().getAmount());
        ArgumentCaptor<AlipayPagePayCommand> command = ArgumentCaptor.forClass(AlipayPagePayCommand.class);
        verify(gateway).pagePay(command.capture());
        assertEquals(saved.getValue().getPayNo(), command.getValue().outTradeNo());
        assertEquals(new BigDecimal("12.00"), command.getValue().totalAmount());
        assertEquals("<form id=alipay></form>", response.getFormHtml());
    }

    @Test
    void rejectsForeignOrderAndNonPositiveAmountBeforeCallingAlipay() {
        Order foreign = order(501L, 12L, OrderStatus.PENDING_PAYMENT, "12.00");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(foreign);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(foreign);
        assertThrows(CustomForbiddenException.class, () -> service.create(foreign.getOrderNo()));

        Order zero = order(502L, 11L, OrderStatus.PENDING_PAYMENT, "0.00");
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(zero);
        when(orderMapper.selectByIdForUpdate(502L)).thenReturn(zero);
        assertThrows(CustomConflictException.class, () -> service.create(zero.getOrderNo()));
        verify(gateway, never()).pagePay(any());
    }

    @Test
    void rejectsUnverifiedNotificationWithoutReadingPayment() {
        Map<String, String> parameters = notification("12.00");
        when(gateway.verifyNotification(parameters)).thenReturn(false);

        assertFalse(service.notify(parameters));

        verify(paymentMapper, never()).selectByPayNo(any());
    }

    @Test
    void validNotificationMarksPaymentAndOrderPaid() {
        Map<String, String> parameters = notification("12.00");
        Payment payment = payment(PaymentStatus.PENDING);
        Order order = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        when(gateway.verifyNotification(parameters)).thenReturn(true);
        when(paymentMapper.selectByPayNo("PAY7001")).thenReturn(payment);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(order);
        when(paymentMapper.selectByIdForUpdate(7001L)).thenReturn(payment);
        when(paymentMapper.selectByTradeNo("ALI9001")).thenReturn(null);
        when(paymentMapper.updateById(payment)).thenReturn(1);
        when(orderService.markPaid(eq(501L), any())).thenReturn(true);

        assertTrue(service.notify(parameters));

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("ALI9001", payment.getTradeNo());
        assertEquals("ALIPAY", payment.getChannel());
        assertTrue(payment.getPaidAt() != null);
        assertTrue(payment.getNotifiedAt() != null);
        verify(orderService).markPaid(501L, payment.getPaidAt());
    }

    @Test
    void notificationRejectsAmountMismatchAndIsIdempotentAfterSuccess() {
        Map<String, String> wrongAmount = notification("11.99");
        Payment pending = payment(PaymentStatus.PENDING);
        Order order = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        when(gateway.verifyNotification(wrongAmount)).thenReturn(true);
        when(paymentMapper.selectByPayNo("PAY7001")).thenReturn(pending);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(order);
        when(paymentMapper.selectByIdForUpdate(7001L)).thenReturn(pending);
        assertThrows(CustomConflictException.class, () -> service.notify(wrongAmount));

        Map<String, String> repeated = notification("12.00");
        Payment success = payment(PaymentStatus.SUCCESS);
        success.setTradeNo("ALI9001");
        Order shipped = order(501L, 11L, OrderStatus.SHIPPED, "12.00");
        when(gateway.verifyNotification(repeated)).thenReturn(true);
        when(paymentMapper.selectByPayNo("PAY7001")).thenReturn(success);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(shipped);
        when(paymentMapper.selectByIdForUpdate(7001L)).thenReturn(success);

        assertTrue(service.notify(repeated));
        verify(paymentMapper, never()).updateById(any(Payment.class));
        verify(orderService, never()).markPaid(any(), any());
    }

    @Test
    void activeQueryCompensatesForMissedNotification() {
        Order order = order(501L, 11L, OrderStatus.PENDING_PAYMENT, "12.00");
        Payment payment = payment(PaymentStatus.PENDING);
        when(orderMapper.selectOne(any(Wrapper.class))).thenReturn(order);
        when(paymentMapper.selectByOrderId(501L)).thenReturn(payment);
        when(gateway.query("PAY7001")).thenReturn(new AlipayTradeQueryResult(
                true, "PAY7001", "ALI9001", new BigDecimal("12.00"), "TRADE_SUCCESS"));
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(order);
        when(paymentMapper.selectByIdForUpdate(7001L)).thenReturn(payment);
        when(paymentMapper.selectByTradeNo("ALI9001")).thenReturn(null);
        when(paymentMapper.updateById(payment)).thenReturn(1);
        when(orderService.markPaid(eq(501L), any())).thenReturn(true);

        PaymentResponse response = service.query(order.getOrderNo());

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(OrderStatus.PAID, response.getOrderStatus());
        assertEquals("ALI9001", response.getTradeNo());
    }

    private Map<String, String> notification(String amount) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("app_id", "sandbox-app");
        values.put("seller_id", "sandbox-seller");
        values.put("out_trade_no", "PAY7001");
        values.put("trade_no", "ALI9001");
        values.put("total_amount", amount);
        values.put("trade_status", "TRADE_SUCCESS");
        values.put("notify_id", "notify-1");
        values.put("sign", "signed");
        return values;
    }

    private Order order(Long id, Long userId, OrderStatus status, String amount) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo("ORDER" + id);
        order.setUserId(userId);
        order.setStatus(status);
        order.setPayAmount(new BigDecimal(amount));
        return order;
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(7001L);
        payment.setOrderId(501L);
        payment.setOrderNo("ORDER501");
        payment.setPayNo("PAY7001");
        payment.setAmount(new BigDecimal("12.00"));
        payment.setChannel("ALIPAY");
        payment.setStatus(status);
        return payment;
    }
}
