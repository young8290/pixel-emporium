package com.example.business;

import com.example.business.entity.ProductSku;
import com.example.business.service.ProductSkuService;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("mysql-integration")
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "alibaba.cloud.oss.access-key=test-access-key",
        "alibaba.cloud.oss.secret-key=test-secret-key"
})
class StockConcurrencyTest {

    @Autowired
    private ProductSkuService productSkuService;

    private final List<Long> createdIds = Collections.synchronizedList(new ArrayList<>());

    @AfterEach
    void cleanUp() {
        if (!createdIds.isEmpty()) {
            productSkuService.removeByIds(createdIds);
            createdIds.clear();
        }
    }

    @Test
    void concurrentDeductionNeverOversells() throws Exception {
        ProductSku sku = createSku(100);
        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Future<?>> tasks = new ArrayList<>();

        for (int i = 0; i < 200; i++) {
            tasks.add(executor.submit(() -> {
                start.await();
                try {
                    productSkuService.deductStock(sku.getId(), 1);
                    successes.incrementAndGet();
                } catch (CustomBusinessException ignored) {
                    // Expected after the stock reaches zero.
                }
                return null;
            }));
        }

        start.countDown();
        for (Future<?> task : tasks) {
            task.get();
        }
        executor.shutdown();

        assertEquals(100, successes.get());
        assertEquals(0, productSkuService.getById(sku.getId()).getStock());
        assertTrue(productSkuService.getById(sku.getId()).getStock() >= 0);
    }

    @Test
    void insufficientStockRaisesBusinessException() {
        ProductSku sku = createSku(1);

        assertThrows(CustomBusinessException.class,
                () -> productSkuService.deductStock(sku.getId(), 2));
    }

    @Test
    void restoreStockReturnsTheDeductedQuantity() {
        ProductSku sku = createSku(10);

        productSkuService.deductStock(sku.getId(), 5);
        productSkuService.restoreStock(sku.getId(), 5);

        assertEquals(10, productSkuService.getById(sku.getId()).getStock());
    }

    @Test
    void nonPositiveQuantityRaisesValidationException() {
        ProductSku sku = createSku(10);

        assertThrows(CustomValidationException.class,
                () -> productSkuService.deductStock(sku.getId(), 0));
        assertThrows(CustomValidationException.class,
                () -> productSkuService.restoreStock(sku.getId(), -1));
    }

    private ProductSku createSku(int stock) {
        ProductSku sku = new ProductSku();
        sku.setTitle("stock-test");
        sku.setDescription("stock-test");
        sku.setPrice(new BigDecimal("1.00"));
        sku.setStock(stock);
        sku.setProductId(1L);
        productSkuService.save(sku);
        createdIds.add(sku.getId());
        return sku;
    }
}
