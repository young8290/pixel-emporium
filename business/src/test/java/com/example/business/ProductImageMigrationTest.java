package com.example.business;

import com.example.business.entity.ProductImage;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductImageMigrationTest {
    @Test
    void migrationDefinesProductImageStateAndRollback() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V6_2__product_image.sql");
        assertTrue(Files.exists(migration));
        String sql = Files.readString(migration).toLowerCase();

        assertTrue(sql.contains("create table if not exists `product_image`"));
        assertTrue(sql.contains("style_status"));
        assertTrue(sql.contains("original_url"));
        assertTrue(sql.contains("styled_url"));
        assertTrue(sql.contains("idx_pi_product_deleted"));
        assertTrue(sql.contains("idx_pi_sku"));
        assertTrue(sql.contains("-- rollback"));
        assertTrue(sql.contains("drop table if exists `product_image`"));
    }

    @Test
    void productImageExposesTheFourStateMachineValues() {
        assertTrue(ProductImage.StyleStatus.NONE.name().equals("NONE"));
        assertTrue(ProductImage.StyleStatus.PENDING.name().equals("PENDING"));
        assertTrue(ProductImage.StyleStatus.DONE.name().equals("DONE"));
        assertTrue(ProductImage.StyleStatus.FAILED.name().equals("FAILED"));
    }
}
