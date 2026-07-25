package com.example.business.service;

import com.example.business.response.AlipayPaymentResponse;
import com.example.business.response.PaymentResponse;

import java.util.Map;

public interface AlipayPaymentService {
    AlipayPaymentResponse create(String orderNo);

    boolean notify(Map<String, String> parameters);

    PaymentResponse query(String orderNo);
}
