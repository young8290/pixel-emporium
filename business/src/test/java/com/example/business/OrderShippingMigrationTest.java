package com.example.business;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderShippingMigrationTest {
    @Test
    void migrationAddsSimpleShippingSnapshotFields() throws Exception {
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V18__order_shipping.sql")) {
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("shipping_carrier"));
            assertTrue(sql.contains("tracking_number"));
            assertTrue(sql.contains("shipping_note"));
        }
    }
}
