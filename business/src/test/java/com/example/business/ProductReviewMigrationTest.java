package com.example.business;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.business.entity.ProductReview;
import com.example.business.enums.ProductReviewStatus;
import com.example.business.mapper.ProductReviewMapper;
import com.example.business.model.ProductReviewAggregate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductReviewMigrationTest {
    @Test
    void migrationDefinesReviewConstraintsIndexesAndRealAggregates() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V14__product_review.sql");
        assertTrue(Files.exists(migration));

        String sql = Files.readString(migration).toLowerCase().replaceAll("\\s+", " ");
        assertTrue(sql.contains("create table if not exists product_review"));
        assertTrue(sql.contains("unique key uk_review_order_item (order_item_id, deleted)"));
        assertTrue(sql.contains("constraint chk_review_rating check (rating between 1 and 5)"));
        assertTrue(sql.contains("key idx_review_product_public (product_id, status, created_at, id)"));
        assertTrue(sql.contains("key idx_review_user (user_id, created_at, id)"));
        assertTrue(sql.contains("update product set rating = 0.00, review_count = 0"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("drop product"));
        assertFalse(sql.contains("drop order_item"));
    }

    @Test
    void persistenceModelExposesReviewOwnershipAndModerationFields() throws Exception {
        assertEquals("product_review", ProductReview.class.getAnnotation(TableName.class).value());
        assertEquals(Long.class, ProductReview.class.getDeclaredField("productId").getType());
        assertEquals(Long.class, ProductReview.class.getDeclaredField("orderId").getType());
        assertEquals(Long.class, ProductReview.class.getDeclaredField("orderItemId").getType());
        assertEquals(Long.class, ProductReview.class.getDeclaredField("userId").getType());
        assertEquals(Integer.class, ProductReview.class.getDeclaredField("rating").getType());
        assertEquals(String.class, ProductReview.class.getDeclaredField("content").getType());
        assertEquals(ProductReviewStatus.class, ProductReview.class.getDeclaredField("status").getType());
        assertArrayEquals(new ProductReviewStatus[]{
                ProductReviewStatus.PUBLISHED,
                ProductReviewStatus.HIDDEN
        }, ProductReviewStatus.values());
        assertTrue(com.baomidou.mybatisplus.core.mapper.BaseMapper.class
                .isAssignableFrom(ProductReviewMapper.class));

        ProductReviewAggregate aggregate = new ProductReviewAggregate(3L, new BigDecimal("4.67"));
        assertEquals(3L, aggregate.count());
        assertEquals(new BigDecimal("4.67"), aggregate.averageRating());
    }
}
