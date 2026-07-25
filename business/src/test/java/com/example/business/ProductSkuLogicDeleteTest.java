package com.example.business;

import com.example.business.entity.ProductSku;
import com.example.business.service.ProductSkuService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("mysql-integration")
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "alibaba.cloud.oss.access-key=test-access-key",
        "alibaba.cloud.oss.secret-key=test-secret-key"
})
@Transactional
class ProductSkuLogicDeleteTest {

    @Autowired
    private ProductSkuService productSkuService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void removeByIdMarksRowDeletedAndHidesItFromNormalQueries() {
        ProductSku sku = new ProductSku();
        sku.setTitle("logic-delete-test");
        sku.setDescription("logic-delete-test");
        sku.setPrice(new BigDecimal("12.34"));
        sku.setStock(10);
        sku.setProductId(1L);

        assertTrue(productSkuService.save(sku));
        assertTrue(productSkuService.removeById(sku.getId()));

        assertNull(productSkuService.getById(sku.getId()));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "select deleted from product_sku where id = ?",
                Long.class,
                sku.getId()));
    }

    @Test
    void deletingMissingIdOrEmptyBatchReturnsFalse() {
        assertFalse(productSkuService.removeById(Long.MAX_VALUE));
        assertFalse(productSkuService.removeByIds(List.of()));
    }
}
