package com.example.business.response;

import com.example.business.enums.OrderStatus;
import com.example.business.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentResponse {
    private String payNo;
    private String tradeNo;
    private String orderNo;
    private BigDecimal amount;
    private String channel;
    private PaymentStatus status;
    private OrderStatus orderStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime paidAt;

    private boolean mockPayment;
    private boolean confirmable;
}
