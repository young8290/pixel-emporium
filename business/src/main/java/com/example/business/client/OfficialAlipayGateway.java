package com.example.business.client;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.example.business.config.AlipayProperties;
import com.example.common.exception.CustomBusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class OfficialAlipayGateway implements AlipayGateway {
    private final AlipayProperties properties;

    public OfficialAlipayGateway(AlipayProperties properties) {
        this.properties = properties;
    }

    @Override
    public String pagePay(AlipayPagePayCommand command) {
        requireText(properties.getNotifyUrl(), "支付宝异步通知地址未配置");
        requireText(properties.getReturnUrl(), "支付宝同步返回地址未配置");
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(command.outTradeNo());
        model.setTotalAmount(command.totalAmount().setScale(2).toPlainString());
        model.setSubject(command.subject());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(properties.getNotifyUrl());
        request.setReturnUrl(properties.getReturnUrl());
        request.setBizModel(model);
        try {
            return client().pageExecute(request).getBody();
        } catch (AlipayApiException exception) {
            throw new CustomBusinessException("支付宝支付表单生成失败");
        }
    }

    @Override
    public boolean verifyNotification(Map<String, String> parameters) {
        requireClientConfiguration();
        try {
            return AlipaySignature.rsaCheckV1(new HashMap<>(parameters),
                    properties.getAlipayPublicKey(), properties.getCharset(), properties.getSignType());
        } catch (AlipayApiException exception) {
            return false;
        }
    }

    @Override
    public AlipayTradeQueryResult query(String outTradeNo) {
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizModel(model);
        try {
            AlipayTradeQueryResponse response = client().execute(request);
            return new AlipayTradeQueryResult(response.isSuccess(), response.getOutTradeNo(),
                    response.getTradeNo(), decimal(response.getTotalAmount()), response.getTradeStatus());
        } catch (AlipayApiException exception) {
            throw new CustomBusinessException("支付宝订单查询失败");
        }
    }

    private AlipayClient client() {
        requireClientConfiguration();
        return new DefaultAlipayClient(properties.getGatewayUrl(), properties.getAppId(),
                properties.getPrivateKey(), "json", properties.getCharset(),
                properties.getAlipayPublicKey(), properties.getSignType());
    }

    private void requireClientConfiguration() {
        if (!properties.isEnabled()) {
            throw new CustomBusinessException("支付宝沙箱支付未启用");
        }
        requireText(properties.getGatewayUrl(), "支付宝网关未配置");
        requireText(properties.getAppId(), "支付宝应用 ID 未配置");
        requireText(properties.getPrivateKey(), "支付宝应用私钥未配置");
        requireText(properties.getAlipayPublicKey(), "支付宝公钥未配置");
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new CustomBusinessException(message);
        }
    }

    private BigDecimal decimal(String value) {
        return StringUtils.hasText(value) ? new BigDecimal(value) : null;
    }
}
