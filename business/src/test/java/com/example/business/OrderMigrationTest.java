package com.example.business;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.example.common.orm.config.MybatisPlusConfig;
import com.example.business.entity.Order;
import com.example.business.entity.OrderItem;
import com.example.business.enums.OrderStatus;
import com.example.business.mapper.OrderMapper;
import com.example.business.request.OrderCreateRequest;
import com.example.business.request.OrderListRequest;
import com.example.business.response.OrderItemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderMigrationTest {
    @Test
    void migrationDefinesOrderTablesIndexesAndRollback() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V8__order.sql");
        assertTrue(Files.exists(migration));
        String sql = Files.readString(migration).toLowerCase().replaceAll("\\s+", " ");

        assertTrue(sql.contains("create table if not exists `order`"));
        assertTrue(sql.contains("create table if not exists `order_item`"));
        assertTrue(sql.contains("unique key `uk_order_no`"));
        assertTrue(sql.contains("key `idx_user_status` (`user_id`, `status`)"));
        assertTrue(sql.contains("key `idx_order` (`order_id`)"));
        assertTrue(sql.contains("`total_amount` decimal(10, 2)"));
        assertTrue(sql.contains("`pay_amount` decimal(10, 2)"));
        assertTrue(sql.contains("`freight_amount` decimal(10, 2)"));
        assertTrue(sql.contains("`paid_at` datetime"));
        assertTrue(sql.contains("`shipped_at` datetime"));
        assertTrue(sql.contains("`completed_at` datetime"));
        assertTrue(sql.contains("`canceled_at` datetime"));

        int rollback = sql.indexOf("-- rollback:");
        int dropItems = sql.indexOf("drop table if exists `order_item`", rollback);
        int dropOrder = sql.indexOf("drop table if exists `order`", rollback);
        assertTrue(rollback >= 0 && dropItems > rollback && dropOrder > dropItems);
    }

    @Test
    void entitiesExposeThePersistedOrderContract() throws Exception {
        assertEquals("`order`", Order.class.getAnnotation(TableName.class).value());
        assertEquals(String.class, Order.class.getDeclaredField("orderNo").getType());
        assertEquals(Long.class, Order.class.getDeclaredField("userId").getType());
        assertEquals(OrderStatus.class, Order.class.getDeclaredField("status").getType());
        assertEquals(BigDecimal.class, Order.class.getDeclaredField("totalAmount").getType());
        assertEquals(BigDecimal.class, Order.class.getDeclaredField("payAmount").getType());
        assertEquals(BigDecimal.class, Order.class.getDeclaredField("freightAmount").getType());
        assertEquals(Long.class, OrderItem.class.getDeclaredField("orderId").getType());
        assertEquals(Long.class, OrderItem.class.getDeclaredField("productSkuId").getType());
        assertEquals(BigDecimal.class, OrderItem.class.getDeclaredField("price").getType());
        assertEquals(Integer.class, OrderItem.class.getDeclaredField("quantity").getType());
    }

    @Test
    void requestResponseAndMapperContractsAreStable() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            OrderCreateRequest valid = new OrderCreateRequest();
            valid.setAddressId(1L);
            valid.setSource("buyNow");
            valid.setSkuId(2L);
            valid.setQuantity(1);
            assertTrue(validator.validate(valid).isEmpty());

            valid.setSource("unknown");
            assertTrue(validator.validate(valid).stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("source")));
        }

        OrderListRequest listRequest = new OrderListRequest();
        listRequest.setStatus(OrderStatus.PENDING_PAYMENT);
        assertEquals(1, listRequest.getPage());
        assertEquals(OrderStatus.PENDING_PAYMENT, listRequest.getStatus());

        OrderItemResponse item = new OrderItemResponse();
        item.setId(9007199254740993L);
        String json = new ObjectMapper().writeValueAsString(item);
        assertTrue(json.contains("\"id\":\"9007199254740993\""));

        Method address = OrderMapper.class.getMethod("selectAddressById", Long.class);
        Method locked = OrderMapper.class.getMethod("selectByIdForUpdate", Long.class);
        Method timeout = OrderMapper.class.getMethod(
                "selectTimeoutIds", LocalDateTime.class, int.class);
        String addressSql = selectSql(address);
        String lockedSql = selectSql(locked);
        String timeoutSql = selectSql(timeout);
        assertTrue(addressSql.contains("from address"));
        assertTrue(addressSql.contains("deleted = 0"));
        assertTrue(lockedSql.contains("from `order`"));
        assertTrue(lockedSql.contains("for update"));
        assertTrue(timeoutSql.contains("status = 'pending_payment'"));
        assertTrue(timeoutSql.contains("created_at < #{deadline}"));
        assertTrue(timeoutSql.contains("order by created_at asc, id asc"));
        assertTrue(timeoutSql.contains("limit #{limit}"));
    }

    @Test
    void businessProvidesIdentifierGeneratorWithoutSpringCloud() {
        IdentifierGenerator generator = new MybatisPlusConfig().identifierGenerator();

        assertTrue(generator.nextId(this).longValue() > 0);
    }

    private String selectSql(Method method) {
        Select select = method.getAnnotation(Select.class);
        return String.join(" ", select.value()).toLowerCase().replaceAll("\\s+", " ");
    }
}
