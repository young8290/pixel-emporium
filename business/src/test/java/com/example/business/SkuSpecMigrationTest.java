package com.example.business;

import com.example.business.entity.ProductSku;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SkuSpecMigrationTest {
    @Test
    void migrationDefinesSpecificationTablesIndexesAndRollback() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V6_1__sku_spec.sql");
        assertTrue(Files.exists(migration));
        String sql = Files.readString(migration).toLowerCase();

        assertTrue(sql.contains("create table if not exists `product_attribute`"));
        assertTrue(sql.contains("create table if not exists `product_attribute_value`"));
        assertTrue(sql.contains("create table if not exists `sku_attribute_value`"));
        assertTrue(sql.contains("alter table `product_sku`"));
        assertTrue(sql.contains("uk_product_skucode"));
        assertTrue(sql.contains("uk_sku_attr"));
        assertTrue(sql.contains("drop table"));
        assertTrue(sql.contains("drop column `sku_code`"));
    }

    @Test
    void productSkuExposesStableSpecificationFields() throws Exception {
        Field skuCode = ProductSku.class.getDeclaredField("skuCode");
        Field mainImageId = ProductSku.class.getDeclaredField("mainImageId");

        assertTrue(skuCode.getType().equals(String.class));
        assertTrue(mainImageId.getType().equals(Long.class));
    }
}
