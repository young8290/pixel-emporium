package com.example.business.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductSkuServiceSecurityTest {
    @Test
    void serviceDoesNotExposeRemovedPublicWriteCompatibilityMethods() {
        Set<String> legacyWriteMethods = Set.of(
                "saveOrUpdateProductSku",
                "deleteProductSku",
                "adminDelete",
                "saveOrUpdate"
        );

        assertTrue(Arrays.stream(ProductSkuService.class.getDeclaredMethods())
                        .map(method -> method.getName())
                        .noneMatch(legacyWriteMethods::contains),
                "旧的公开 SKU 写服务方法必须删除");
    }
}
