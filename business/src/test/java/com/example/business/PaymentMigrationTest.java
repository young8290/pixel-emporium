package com.example.business;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.business.entity.Payment;
import com.example.business.config.AlipayProperties;
import com.example.business.enums.OrderStatus;
import com.example.business.enums.PaymentStatus;
import com.example.business.mapper.PaymentMapper;
import com.example.business.response.PaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentMigrationTest {
    @Test
    void migrationDefinesPaymentFieldsIndexesAndRollback() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V9__payment.sql");
        assertTrue(Files.exists(migration));
        String sql = Files.readString(migration).toLowerCase().replaceAll("\\s+", " ");

        assertTrue(sql.contains("create table if not exists `payment`"));
        assertTrue(sql.contains("`order_id` bigint"));
        assertTrue(sql.contains("`order_no` varchar(32)"));
        assertTrue(sql.contains("`pay_no` varchar(32)"));
        assertTrue(sql.contains("`amount` decimal(10, 2)"));
        assertTrue(sql.contains("`channel` varchar(20) not null default 'mock'"));
        assertTrue(sql.contains("`status` varchar(20)"));
        assertTrue(sql.contains("`paid_at` datetime"));
        assertTrue(sql.contains("unique key `uk_payment_pay_no` (`pay_no`)"));
        assertTrue(sql.contains("unique key `uk_payment_order` (`order_id`)"));
        assertTrue(sql.contains("key `idx_order` (`order_id`)"));
        int rollback = sql.indexOf("-- rollback:");
        assertTrue(rollback >= 0);
        assertTrue(sql.indexOf("drop table if exists `payment`", rollback) > rollback);
    }

    @Test
    void modelExposesPaymentPersistenceAndResponseContract() throws Exception {
        assertArrayEquals(new PaymentStatus[]{
                PaymentStatus.PENDING,
                PaymentStatus.SUCCESS,
                PaymentStatus.FAILED
        }, PaymentStatus.values());
        assertEquals("payment", Payment.class.getAnnotation(TableName.class).value());
        assertEquals(Long.class, Payment.class.getDeclaredField("orderId").getType());
        assertEquals(String.class, Payment.class.getDeclaredField("orderNo").getType());
        assertEquals(String.class, Payment.class.getDeclaredField("payNo").getType());
        assertEquals(BigDecimal.class, Payment.class.getDeclaredField("amount").getType());
        assertEquals(String.class, Payment.class.getDeclaredField("channel").getType());
        assertEquals(PaymentStatus.class, Payment.class.getDeclaredField("status").getType());

        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.PENDING);
        response.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        response.setMockPayment(true);
        response.setConfirmable(true);
        String json = new ObjectMapper().writeValueAsString(response);
        assertTrue(json.contains("\"status\":\"PENDING\""));
        assertTrue(json.contains("\"orderStatus\":\"PENDING_PAYMENT\""));
        assertTrue(json.contains("\"mockPayment\":true"));
        assertTrue(json.contains("\"confirmable\":true"));
    }

    @Test
    void mapperQueriesExcludeDeletedRowsAndExposeRequiredLocks() throws Exception {
        assertTrue(selectSql(PaymentMapper.class.getMethod(
                "selectByOrderId", Long.class)).contains("deleted = 0"));
        assertTrue(selectSql(PaymentMapper.class.getMethod(
                "selectByOrderIdForUpdate", Long.class)).contains("for update"));
        assertTrue(selectSql(PaymentMapper.class.getMethod(
                "selectByPayNo", String.class)).contains("deleted = 0"));
        assertTrue(selectSql(PaymentMapper.class.getMethod(
                "selectByIdForUpdate", Long.class)).contains("for update"));
    }

    @Test
    void alipayMigrationAndModelExposeProviderIdentifiers() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V15__alipay_payment.sql");
        assertTrue(Files.exists(migration));
        String sql = Files.readString(migration).toLowerCase().replaceAll("\\s+", " ");

        assertTrue(sql.contains("add column `trade_no` varchar(64)"));
        assertTrue(sql.contains("add column `notified_at` datetime"));
        assertTrue(sql.contains("add column `notification_summary` varchar(255)"));
        assertTrue(sql.contains("unique key `uk_payment_trade_no` (`trade_no`)"));
        assertTrue(sql.contains("drop index `uk_payment_trade_no`"));

        assertEquals(String.class, Payment.class.getDeclaredField("tradeNo").getType());
        assertEquals(LocalDateTime.class, Payment.class.getDeclaredField("notifiedAt").getType());
        assertEquals(String.class, Payment.class.getDeclaredField("notificationSummary").getType());
        assertTrue(selectSql(PaymentMapper.class.getMethod(
                "selectByTradeNo", String.class)).contains("trade_no = #{tradeno}"));
    }

    @Test
    void alipayConfigurationUsesSandboxDefaultsAndEnvironmentPlaceholders() throws Exception {
        AlipayProperties properties = new AlipayProperties();
        assertEquals("https://openapi-sandbox.dl.alipaydev.com/gateway.do", properties.getGatewayUrl());
        assertEquals("RSA2", properties.getSignType());

        String yaml = Files.readString(Path.of("src/main/resources/application.yml"));
        assertTrue(yaml.contains("app-id: ${ALIPAY_APP_ID:}"));
        assertTrue(yaml.contains("private-key: ${ALIPAY_PRIVATE_KEY:}"));
        assertTrue(yaml.contains("alipay-public-key: ${ALIPAY_PUBLIC_KEY:}"));
        assertTrue(yaml.contains("notify-url: ${ALIPAY_NOTIFY_URL:}"));
        assertTrue(yaml.contains("return-url: ${ALIPAY_RETURN_URL:}"));
    }

    private String selectSql(Method method) {
        Select select = method.getAnnotation(Select.class);
        return String.join(" ", select.value()).toLowerCase().replaceAll("\\s+", " ");
    }
}
