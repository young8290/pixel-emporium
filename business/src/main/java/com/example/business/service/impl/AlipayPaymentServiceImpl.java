package com.example.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.example.business.service.AlipayPaymentService;
import com.example.business.service.OrderService;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomForbiddenException;
import com.example.common.exception.CustomValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class AlipayPaymentServiceImpl implements AlipayPaymentService {
    private static final String CHANNEL = "ALIPAY";

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final IdentifierGenerator identifierGenerator;
    private final AlipayGateway gateway;
    private final AlipayProperties properties;

    public AlipayPaymentServiceImpl(PaymentMapper paymentMapper,
                                    OrderMapper orderMapper,
                                    OrderService orderService,
                                    IdentifierGenerator identifierGenerator,
                                    AlipayGateway gateway,
                                    AlipayProperties properties) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.identifierGenerator = identifierGenerator;
        this.gateway = gateway;
        this.properties = properties;
    }

    @Override
    @Transactional
    public AlipayPaymentResponse create(String rawOrderNo) {
        requireEnabled();
        Long userId = requireUserId();
        String orderNo = requireText(rawOrderNo, "订单号不能为空");
        Order order = loadLockedOrderByNo(orderNo);
        requireOwned(order, userId);
        Payment payment = paymentMapper.selectByOrderIdForUpdate(order.getId());
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCESS) {
            return response(payment, "");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new CustomConflictException("当前订单状态不允许发起支付");
        }
        requirePositiveAmount(order.getPayAmount());
        if (payment == null) {
            payment = buildPayment(order);
            if (paymentMapper.insert(payment) != 1) {
                throw new CustomBusinessException("支付记录保存失败");
            }
        } else if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new CustomConflictException("当前支付状态不允许重复发起");
        } else if (!CHANNEL.equals(payment.getChannel())) {
            payment.setChannel(CHANNEL);
            updatePayment(payment);
        }

        String formHtml = gateway.pagePay(new AlipayPagePayCommand(payment.getPayNo(),
                payment.getAmount(), "像素商城订单 " + payment.getOrderNo()));
        return response(payment, formHtml);
    }

    @Override
    @Transactional
    public boolean notify(Map<String, String> parameters) {
        requireEnabled();
        if (parameters == null || !gateway.verifyNotification(parameters)) {
            return false;
        }
        String appId = requireText(parameters.get("app_id"), "支付宝通知缺少应用 ID");
        if (!Objects.equals(properties.getAppId(), appId)) {
            throw new CustomConflictException("支付宝通知应用 ID 不匹配");
        }
        if (StringUtils.hasText(properties.getSellerId())
                && !Objects.equals(properties.getSellerId(), parameters.get("seller_id"))) {
            throw new CustomConflictException("支付宝通知商户 ID 不匹配");
        }
        String tradeStatus = requireText(parameters.get("trade_status"), "支付宝通知缺少交易状态");
        if (!isPaidStatus(tradeStatus)) {
            return false;
        }
        String payNo = requireText(parameters.get("out_trade_no"), "支付宝通知缺少商户订单号");
        String tradeNo = requireText(parameters.get("trade_no"), "支付宝通知缺少交易号");
        BigDecimal amount = parseAmount(parameters.get("total_amount"));

        Payment observed = paymentMapper.selectByPayNo(payNo);
        if (observed == null) {
            throw new NoSuchElementException("支付记录不存在");
        }
        Order order = orderMapper.selectByIdForUpdate(observed.getOrderId());
        Payment payment = paymentMapper.selectByIdForUpdate(observed.getId());
        requireLockedPair(payNo, order, payment);
        applySuccessfulTrade(payment, order, tradeNo, amount,
                notificationSummary(parameters, tradeStatus), true);
        return true;
    }

    @Override
    @Transactional
    public PaymentResponse query(String rawOrderNo) {
        requireEnabled();
        Long userId = requireUserId();
        String orderNo = requireText(rawOrderNo, "订单号不能为空");
        Order observedOrder = loadOrderByNo(orderNo);
        requireOwned(observedOrder, userId);
        Payment observedPayment = paymentMapper.selectByOrderId(observedOrder.getId());
        if (observedPayment == null) {
            throw new NoSuchElementException("支付记录不存在");
        }
        if (observedPayment.getStatus() == PaymentStatus.SUCCESS) {
            return toPaymentResponse(observedPayment, observedOrder);
        }

        AlipayTradeQueryResult result = gateway.query(observedPayment.getPayNo());
        if (!result.success() || !isPaidStatus(result.tradeStatus())) {
            return toPaymentResponse(observedPayment, observedOrder);
        }
        if (!Objects.equals(observedPayment.getPayNo(), result.outTradeNo())) {
            throw new CustomConflictException("支付宝查询订单号不匹配");
        }
        Order order = orderMapper.selectByIdForUpdate(observedOrder.getId());
        Payment payment = paymentMapper.selectByIdForUpdate(observedPayment.getId());
        requireLockedPair(observedPayment.getPayNo(), order, payment);
        applySuccessfulTrade(payment, order, requireText(result.tradeNo(), "支付宝查询缺少交易号"),
                result.totalAmount(), "source=query;status=" + result.tradeStatus(), false);
        return toPaymentResponse(payment, order);
    }

    private void applySuccessfulTrade(Payment payment,
                                      Order order,
                                      String tradeNo,
                                      BigDecimal amount,
                                      String summary,
                                      boolean notification) {
        requireMatchingAmount(amount, order, payment);
        Payment tradeOwner = paymentMapper.selectByTradeNo(tradeNo);
        if (tradeOwner != null && !Objects.equals(tradeOwner.getId(), payment.getId())) {
            throw new CustomConflictException("支付宝交易号已绑定其他订单");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (!Objects.equals(payment.getTradeNo(), tradeNo)) {
                throw new CustomConflictException("支付记录交易号不一致");
            }
            return;
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new CustomConflictException("当前支付状态不允许确认支付");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT && order.getStatus() != OrderStatus.PAID) {
            throw new CustomConflictException("当前订单状态不允许支付");
        }

        LocalDateTime paidAt = order.getPaidAt() == null ? LocalDateTime.now() : order.getPaidAt();
        payment.setChannel(CHANNEL);
        payment.setTradeNo(tradeNo);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(paidAt);
        payment.setNotificationSummary(limit(summary, 255));
        if (notification) {
            payment.setNotifiedAt(LocalDateTime.now());
        }
        updatePayment(payment);
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            if (!orderService.markPaid(order.getId(), paidAt)) {
                throw new CustomBusinessException("订单支付状态更新失败");
            }
            order.setStatus(OrderStatus.PAID);
            order.setPaidAt(paidAt);
        }
    }

    private Payment buildPayment(Order order) {
        Payment payment = new Payment();
        Number generated = identifierGenerator.nextId(payment);
        if (generated == null) {
            throw new CustomBusinessException("支付 ID 生成失败");
        }
        payment.setId(generated.longValue());
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setPayNo("ALI" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + payment.getId());
        payment.setAmount(order.getPayAmount());
        payment.setChannel(CHANNEL);
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    private Order loadOrderByNo(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new NoSuchElementException("订单不存在");
        }
        return order;
    }

    private Order loadLockedOrderByNo(String orderNo) {
        Order observed = loadOrderByNo(orderNo);
        Order locked = orderMapper.selectByIdForUpdate(observed.getId());
        if (locked == null || !Objects.equals(locked.getOrderNo(), orderNo)) {
            throw new NoSuchElementException("订单不存在");
        }
        return locked;
    }

    private void requireLockedPair(String payNo, Order order, Payment payment) {
        if (order == null || payment == null
                || !Objects.equals(payment.getOrderId(), order.getId())
                || !Objects.equals(payment.getPayNo(), payNo)) {
            throw new NoSuchElementException("支付记录不存在");
        }
    }

    private void requireMatchingAmount(BigDecimal amount, Order order, Payment payment) {
        if (amount == null || order.getPayAmount() == null || payment.getAmount() == null
                || amount.compareTo(order.getPayAmount()) != 0
                || amount.compareTo(payment.getAmount()) != 0) {
            throw new CustomConflictException("支付金额不一致");
        }
    }

    private void requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new CustomConflictException("支付宝支付金额必须大于零");
        }
    }

    private BigDecimal parseAmount(String value) {
        try {
            return new BigDecimal(requireText(value, "支付宝通知缺少支付金额"));
        } catch (NumberFormatException exception) {
            throw new CustomValidationException("支付宝通知金额格式错误");
        }
    }

    private boolean isPaidStatus(String status) {
        return "TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status);
    }

    private String notificationSummary(Map<String, String> values, String status) {
        return "source=notify;status=" + status + ";notifyId="
                + Objects.toString(values.get("notify_id"), "");
    }

    private AlipayPaymentResponse response(Payment payment, String formHtml) {
        return new AlipayPaymentResponse(payment.getPayNo(), payment.getOrderNo(),
                payment.getAmount(), formHtml);
    }

    private PaymentResponse toPaymentResponse(Payment payment, Order order) {
        PaymentResponse response = new PaymentResponse();
        response.setPayNo(payment.getPayNo());
        response.setTradeNo(payment.getTradeNo());
        response.setOrderNo(payment.getOrderNo());
        response.setAmount(payment.getAmount());
        response.setChannel(payment.getChannel());
        response.setStatus(payment.getStatus());
        response.setOrderStatus(order.getStatus());
        response.setPaidAt(payment.getPaidAt());
        response.setMockPayment(false);
        response.setConfirmable(payment.getStatus() == PaymentStatus.PENDING
                && order.getStatus() == OrderStatus.PENDING_PAYMENT);
        return response;
    }

    private void updatePayment(Payment payment) {
        if (paymentMapper.updateById(payment) != 1) {
            throw new CustomBusinessException("支付状态更新失败");
        }
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new CustomBusinessException("支付宝沙箱支付未启用");
        }
    }

    private Long requireUserId() {
        Long userId = UserContext.get();
        if (userId == null) {
            throw new CustomValidationException("请先登录");
        }
        return userId;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new CustomValidationException(message);
        }
        return value.trim();
    }

    private void requireOwned(Order order, Long userId) {
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new CustomForbiddenException("无权操作该订单");
        }
    }

    private String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
