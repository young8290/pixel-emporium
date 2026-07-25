package com.example.business.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductSkuControllerSecurityTest {
    @Test
    void publicSkuControllerHasNoWriteEndpoints() {
        boolean hasWriteEndpoint = Arrays.stream(ProductSkuController.class.getDeclaredMethods())
                .map(method -> AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class))
                .filter(mapping -> mapping != null)
                .anyMatch(mapping -> mapping.method().length == 0
                        || !EnumSet.of(RequestMethod.GET, RequestMethod.HEAD, RequestMethod.OPTIONS)
                        .containsAll(Arrays.asList(mapping.method())));

        assertFalse(hasWriteEndpoint, "SKU 写入必须只通过管理员控制器");
    }
}
