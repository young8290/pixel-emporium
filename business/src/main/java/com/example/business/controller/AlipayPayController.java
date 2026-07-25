package com.example.business.controller;

import com.example.business.response.AlipayPaymentResponse;
import com.example.business.response.PaymentResponse;
import com.example.business.service.AlipayPaymentService;
import com.example.common.response.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("pay/alipay")
public class AlipayPayController {
    private static final Logger log = LoggerFactory.getLogger(AlipayPayController.class);
    private final AlipayPaymentService service;

    public AlipayPayController(AlipayPaymentService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public R<AlipayPaymentResponse> create(@RequestBody @Valid CreatePaymentRequest request) {
        return R.ok(service.create(request.orderNo()));
    }

    @PostMapping(value = "/notify",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String notify(@RequestParam Map<String, String> parameters) {
        try {
            return service.notify(parameters) ? "success" : "failure";
        } catch (RuntimeException exception) {
            log.warn("支付宝异步通知处理失败，outTradeNo={}",
                    parameters.get("out_trade_no"), exception);
            return "failure";
        }
    }

    @PostMapping("/query")
    public R<PaymentResponse> query(@RequestParam String orderNo) {
        return R.ok(service.query(orderNo));
    }

    public record CreatePaymentRequest(@NotBlank String orderNo) {
    }
}
