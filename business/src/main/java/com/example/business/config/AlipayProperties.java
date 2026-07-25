package com.example.business.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.alipay")
public class AlipayProperties {
    private boolean enabled;
    private String appId;
    private String sellerId;
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private String privateKey;
    private String alipayPublicKey;
    private String signType = "RSA2";
    private String charset = "UTF-8";
    private String notifyUrl;
    private String returnUrl;
}
