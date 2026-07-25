package com.example.business.request;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductRequestWriteBoundaryTest {

    @Test
    void administratorProductRequestCannotWriteSystemMaintainedCounters() {
        Set<String> writableProperties = Arrays.stream(ProductRequest.class.getMethods())
                .map(method -> method.getName())
                .filter(name -> name.startsWith("set"))
                .collect(java.util.stream.Collectors.toSet());

        assertFalse(writableProperties.contains("setRating"));
        assertFalse(writableProperties.contains("setSoldCount"));
        assertFalse(writableProperties.contains("setReviewCount"));
    }
}
