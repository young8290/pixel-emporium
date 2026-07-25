package com.example.business.controller;

import com.example.business.response.PaymentResponse;
import com.example.business.service.PaymentService;
import com.example.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;

@RestController
@RequestMapping("pay")
public class PayController {
    private final PaymentService paymentService;
    private final boolean mockEnabled;

    public PayController(PaymentService paymentService) {
        this(paymentService, true);
    }

    @Autowired
    public PayController(PaymentService paymentService,
                         @Value("${payment.mock-enabled:false}") boolean mockEnabled) {
        this.paymentService = paymentService;
        this.mockEnabled = mockEnabled;
    }

    @Operation(summary = "创建模拟支付")
    @PostMapping("/create")
    public R<PaymentResponse> create(
            @RequestBody @Valid CreatePaymentRequest request) {
        return R.ok(paymentService.create(request.orderNo()));
    }

    @Operation(summary = "确认模拟支付成功")
    @PostMapping("/callback")
    public R<Boolean> callback(
            @RequestBody @Valid CallbackPaymentRequest request) {
        if (!mockEnabled) {
            throw new com.example.common.exception.CustomForbiddenException("模拟支付接口已关闭");
        }
        return R.ok(paymentService.callback(request.payNo(), request.amount()));
    }

    @Operation(summary = "查询支付状态")
    @GetMapping("/status")
    public R<PaymentResponse> status(@RequestParam String orderNo) {
        return R.ok(paymentService.status(orderNo));
    }

    public record CreatePaymentRequest(@NotBlank String orderNo) {
    }

    public record CallbackPaymentRequest(
            @NotBlank String payNo,
            @NotNull @DecimalMin(value = "0.00") BigDecimal amount) {
    }
}
