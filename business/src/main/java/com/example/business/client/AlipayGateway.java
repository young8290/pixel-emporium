package com.example.business.client;

import java.util.Map;

public interface AlipayGateway {
    String pagePay(AlipayPagePayCommand command);

    boolean verifyNotification(Map<String, String> parameters);

    AlipayTradeQueryResult query(String outTradeNo);
}
