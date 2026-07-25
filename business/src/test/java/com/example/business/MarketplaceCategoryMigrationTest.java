package com.example.business;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketplaceCategoryMigrationTest {
    @Test
    void keepsCategoryIdsStableWhileReplacingOnlyMarketplaceContent() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V12__marketplace_categories.sql");
        if (Files.notExists(migration)) migration = Path.of("business/src/main/resources/db/migration/V12__marketplace_categories.sql");
        String sql = Files.readString(migration);
        String[] expected = {"数码电子|cat-digital.png", "家用电器|cat-appliance.png", "服饰鞋包|cat-apparel.png",
                "家居家具|cat-home.png", "厨房餐厨|cat-kitchen.png", "美妆个护|cat-beauty.png", "图书文具|cat-book.png",
                "玩具乐器|cat-toy.png", "运动户外|cat-sport.png", "母婴用品|cat-baby.png", "宠物生活|cat-pet.png",
                "食品生鲜|cat-grocery.png", "健康保健|cat-health.png", "汽车用品|cat-auto.png", "手作艺术|cat-craft.png",
                "珠宝配饰|cat-jewelry.png", "文具办公|cat-stationery.png", "家装园艺|cat-garden.png", "其他|cat-other.png"};
        for (int index = 0; index < expected.length; index++) {
            for (String part : expected[index].split("\\|")) assertTrue(sql.contains(part));
            assertTrue(sql.contains("where id = " + (900001 + index)));
        }
        assertTrue(sql.contains("update category"));
        assertFalse(sql.contains("create table"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("alter table"));
    }
}
