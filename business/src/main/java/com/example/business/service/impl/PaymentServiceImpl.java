package com.example.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.example.business.entity.Order;
import com.example.business.entity.Payment;
import com.example.business.enums.OrderStatus;
import com.example.business.enums.PaymentStatus;
import com.example.business.mapper.OrderMapper;
import com.example.business.mapper.PaymentMapper;
import com.example.business.response.PaymentResponse;
import com.example.business.service.OrderService;
import com.example.business.service.PaymentService;
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
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class PaymentServiceImpl implements PaymentService {
    private static final String MOCK_CHANNEL = "MOCK";

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final IdentifierGenerator identifierGenerator;

    public PaymentServiceImpl(PaymentMapper paymentMapper,
                              OrderMapper orderMapper,
                              OrderService orderService,
                              IdentifierGenerator identifierGenerator) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.identifierGenerator = identifierGenerator;
    }

    @Override
    @Transactional
    public PaymentResponse create(String rawOrderNo) {
        Long userId = requireUserId();
        String orderNo = requireText(rawOrderNo, "订单号不能为空");
        Order order = loadLockedOrderByNo(orderNo);
        requireOwned(order, userId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new CustomConflictException("当前订单状态不允许发起支付");
        }
        if (order.getPayAmount() == null || order.getPayAmount().signum() < 0) {
            throw new CustomBusinessException("订单应付金额不可用");
        }

        Payment payment = paymentMapper.selectByOrderIdForUpdate(order.getId());
        if (payment == null) {
            payment = buildPayment(order);
            if (paymentMapper.insert(payment) != 1) {
                throw new CustomBusinessException("支付记录保存失败");
            }
        } else if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new CustomConflictException("当前支付状态不允许重复发起");
        }
        return toResponse(payment, order);
    }

    @Override
    @Transactional
    public boolean callback(String rawPayNo, BigDecimal amount) {
        Long userId = requireUserId();
        String payNo = requireText(rawPayNo, "支付流水号不能为空");
        if (amount == null || amount.signum() < 0) {
            throw new CustomValidationException("支付金额不合法");
        }

        Payment observed = paymentMapper.selectByPayNo(payNo);
        if (observed == null) {
            throw new NoSuchElementException("支付记录不存在");
        }
        Order order = orderMapper.selectByIdForUpdate(observed.getOrderId());
        if (order == null) {
            throw new NoSuchElementException("订单不存在");
        }
        requireOwned(order, userId);
        Payment payment = paymentMapper.selectByIdForUpdate(observed.getId());
        if (payment == null
                || !Objects.equals(payment.getOrderId(), order.getId())
                || !Objects.equals(payment.getPayNo(), payNo)) {
            throw new NoSuchElementException("支付记录不存在");
        }
        if (!MOCK_CHANNEL.equals(payment.getChannel())) {
            throw new CustomConflictException("当前支付渠道不允许模拟确认");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return true;
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new CustomConflictException("当前支付状态不允许确认支付");
        }
        requireCallbackAmount(amount, order, payment);

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            LocalDateTime paidAt = LocalDateTime.now();
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(paidAt);
            updatePayment(payment);
            if (!orderService.markPaid(order.getId(), paidAt)) {
                throw new CustomBusinessException("订单支付状态更新失败");
            }
            return true;
        }
        if (order.getStatus() == OrderStatus.PAID) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(order.getPaidAt() == null
                    ? LocalDateTime.now() : order.getPaidAt());
            updatePayment(payment);
            return true;
        }
        throw new CustomConflictException("当前订单状态不允许支付");
    }

    @Override
    public PaymentResponse status(String rawOrderNo) {
        Long userId = requireUserId();
        String orderNo = requireText(rawOrderNo, "订单号不能为空");
        Order order = loadOrderByNo(orderNo);
        requireOwned(order, userId);
        Payment payment = paymentMapper.selectByOrderId(order.getId());
        if (payment == null) {
            throw new NoSuchElementException("支付记录不存在");
        }
        return toResponse(payment, order);
    }

    private Payment buildPayment(Order order) {
        Payment payment = new Payment();
        Number generated = identifierGenerator.nextId(payment);
        if (generated == null) {
            throw new CustomBusinessException("支付ID生成失败");
        }
        payment.setId(generated.longValue());
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setPayNo(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + payment.getId());
        payment.setAmount(order.getPayAmount());
        payment.setChannel(MOCK_CHANNEL);
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    private Order loadOrderByNo(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
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

    private PaymentResponse toResponse(Payment payment, Order order) {
        PaymentResponse response = new PaymentResponse();
        response.setPayNo(payment.getPayNo());
        response.setOrderNo(payment.getOrderNo());
        response.setAmount(payment.getAmount());
        response.setChannel(payment.getChannel());
        response.setStatus(payment.getStatus());
        response.setOrderStatus(order.getStatus());
        response.setPaidAt(payment.getPaidAt());
        response.setMockPayment(MOCK_CHANNEL.equals(payment.getChannel()));
        response.setConfirmable(payment.getStatus() == PaymentStatus.PENDING
                && order.getStatus() == OrderStatus.PENDING_PAYMENT
                && MOCK_CHANNEL.equals(payment.getChannel()));
        return response;
    }

    private void requireCallbackAmount(BigDecimal callback,
                                       Order order,
                                       Payment payment) {
        if (callback == null || callback.signum() < 0) {
            throw new CustomValidationException("支付金额不合法");
        }
        if (order.getPayAmount() == null
                || payment.getAmount() == null
                || callback.compareTo(order.getPayAmount()) != 0
                || callback.compareTo(payment.getAmount()) != 0) {
            throw new CustomConflictException("支付金额不一致");
        }
    }

    private void updatePayment(Payment payment) {
        if (paymentMapper.updateById(payment) != 1) {
            throw new CustomBusinessException("支付状态更新失败");
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
}
