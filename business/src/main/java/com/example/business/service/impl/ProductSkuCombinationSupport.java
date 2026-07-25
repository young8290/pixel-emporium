package com.example.business.service.impl;

import com.example.business.entity.SkuAttributeValue;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ProductSkuCombinationSupport {
    private ProductSkuCombinationSupport() {
    }

    static String key(List<SkuAttributeValue> mappings) {
        return mappings.stream()
                .sorted(Comparator.comparing(SkuAttributeValue::getAttributeId))
                .map(mapping -> mapping.getAttributeId() + "=" + mapping.getAttributeValueId())
                .collect(Collectors.joining("|"));
    }

    static Map<Long, List<SkuAttributeValue>> bySku(List<SkuAttributeValue> mappings) {
        return mappings.stream().collect(Collectors.groupingBy(SkuAttributeValue::getSkuId));
    }
}
