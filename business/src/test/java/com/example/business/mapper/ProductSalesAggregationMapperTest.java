package com.example.business.mapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ProductSalesAggregationMapperTest {

    @Test
    void productMapperExposesPaidOrderSalesAggregation() {
        assertDoesNotThrow(() -> ProductMapper.class
                .getMethod("incrementSoldCountByOrderId", Long.class));
    }
}
