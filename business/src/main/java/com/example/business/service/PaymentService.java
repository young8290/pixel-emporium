package com.example.business.service;

import com.example.business.response.PaymentResponse;

import java.math.BigDecimal;

public interface PaymentService {
    PaymentResponse create(String orderNo);

    boolean callback(String payNo, BigDecimal amount);

    PaymentResponse status(String orderNo);
}
