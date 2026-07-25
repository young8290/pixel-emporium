package com.example.business;

import com.example.business.entity.BrowseHistory;
import com.example.business.entity.ProductFavorite;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductEngagementMigrationTest {
    @Test
    void migrationDefinesFavoriteAndBrowseHistoryIsolationKeys() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V17__favorite_and_browse_history.sql");
        assertTrue(Files.exists(migration));

        String sql = Files.readString(migration).toLowerCase().replaceAll("\\s+", " ");
        assertTrue(sql.contains("create table if not exists `product_favorite`"));
        assertTrue(sql.contains("unique key `uk_favorite_user_product_deleted` (`user_id`, `product_id`, `deleted`)"));
        assertTrue(sql.contains("key `idx_favorite_user_created` (`user_id`, `created_at`, `id`)"));
        assertTrue(sql.contains("create table if not exists `user_browse_history`"));
        assertTrue(sql.contains("unique key `uk_history_user_product_deleted` (`user_id`, `product_id`, `deleted`)"));
        assertTrue(sql.contains("key `idx_history_user_viewed` (`user_id`, `last_viewed_at`, `id`)"));
    }

    @Test
    void persistenceModelsExposeOwnershipAndHistoryFields() throws Exception {
        assertEquals(Long.class, ProductFavorite.class.getDeclaredField("userId").getType());
        assertEquals(Long.class, ProductFavorite.class.getDeclaredField("productId").getType());
        assertEquals(Long.class, BrowseHistory.class.getDeclaredField("userId").getType());
        assertEquals(Long.class, BrowseHistory.class.getDeclaredField("productId").getType());
        assertEquals(Integer.class, BrowseHistory.class.getDeclaredField("viewCount").getType());
        assertEquals(LocalDateTime.class, BrowseHistory.class.getDeclaredField("lastViewedAt").getType());
    }
}
