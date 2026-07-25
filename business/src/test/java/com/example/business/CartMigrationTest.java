package com.example.business;

import com.example.business.entity.Cart;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartMigrationTest {
    @Test
    void migrationDefinesCartKeysAndRollback() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V7__cart.sql");
        assertTrue(Files.exists(migration));
        String sql = Files.readString(migration).toLowerCase();

        assertTrue(sql.contains("create table if not exists `cart`"));
        assertTrue(sql.contains("unique key `uk_cart_user_sku_deleted`"));
        assertTrue(sql.contains("key `idx_user`"));
        assertTrue(sql.contains("drop table if exists `cart`"));
    }

    @Test
    void cartExposesPersistedBusinessFields() throws Exception {
        assertEquals(Long.class, Cart.class.getDeclaredField("userId").getType());
        assertEquals(Long.class, Cart.class.getDeclaredField("productSkuId").getType());
        assertEquals(Integer.class, Cart.class.getDeclaredField("quantity").getType());
        Field checked = Cart.class.getDeclaredField("checked");
        assertEquals(Boolean.class, checked.getType());
    }
}
