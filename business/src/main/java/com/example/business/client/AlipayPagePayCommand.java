package com.example.business.client;

import java.math.BigDecimal;

public record AlipayPagePayCommand(
        String outTradeNo,
        BigDecimal totalAmount,
        String subject) {
}
