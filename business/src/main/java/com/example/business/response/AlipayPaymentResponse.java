package com.example.business.response;

import java.math.BigDecimal;

public record AlipayPaymentResponse(
        String payNo,
        String orderNo,
        BigDecimal amount,
        String formHtml) {
    public String getFormHtml() {
        return formHtml;
    }
}
