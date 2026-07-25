package com.example.business.client;

import java.math.BigDecimal;

public record AlipayTradeQueryResult(
        boolean success,
        String outTradeNo,
        String tradeNo,
        BigDecimal totalAmount,
        String tradeStatus) {
}
