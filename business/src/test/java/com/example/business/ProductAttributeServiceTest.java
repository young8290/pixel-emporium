package com.example.business;

import com.example.business.entity.ProductAttribute;
import com.example.business.entity.ProductAttributeValue;
import com.example.business.entity.SkuAttributeValue;
import com.example.business.mapper.ProductAttributeMapper;
import com.example.business.mapper.ProductAttributeValueMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.mapper.SkuAttributeValueMapper;
import com.example.business.request.ProductAttributeRequest;
import com.example.business.request.ProductAttributeValueRequest;
import com.example.business.service.impl.ProductAttributeServiceImpl;
import com.example.common.exception.CustomValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAttributeServiceTest {
    @Mock
    private ProductAttributeMapper attributeMapper;

    @Mock
    private ProductAttributeValueMapper valueMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductSkuMapper skuMapper;

    @Mock
    private SkuAttributeValueMapper mappingMapper;

    private ProductAttributeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductAttributeServiceImpl(attributeMapper, valueMapper, productMapper,
                skuMapper, mappingMapper);
    }

    @Test
    void listTreeReturnsProductOwnedAttributesAndSortedValues() {
        ProductAttribute first = attribute(11L, 7L, "口味", 1);
        ProductAttribute second = attribute(12L, 7L, "重量", 2);
        ProductAttributeValue value = value(21L, 7L, 11L, "原味", 1);
        when(attributeMapper.selectList(any())).thenReturn(List.of(second, first));
        when(valueMapper.selectList(any())).thenReturn(List.of(value));

        var result = service.listTree(7L);

        assertEquals(List.of("口味", "重量"), result.stream().map(item -> item.getName()).toList());
        assertEquals("原味", result.getFirst().getValues().getFirst().getValue());
    }

    @Test
    void savingValueRejectsAttributeFromAnotherProduct() {
        when(attributeMapper.selectById(11L)).thenReturn(attribute(11L, 8L, "口味", 1));
        ProductAttributeValueRequest request = new ProductAttributeValueRequest();
        request.setProductId(7L);
        request.setAttributeId(11L);
        request.setValue("原味");

        assertThrows(CustomValidationException.class, () -> service.saveValue(request));
        verify(valueMapper, never()).insert(any(ProductAttributeValue.class));
    }

    @Test
    void deletingAttributeRejectsAnotherProduct() {
        when(attributeMapper.selectById(11L)).thenReturn(attribute(11L, 8L, "口味", 1));

        assertThrows(CustomValidationException.class, () -> service.deleteAttribute(7L, 11L));
        verify(attributeMapper, never()).deleteById(11L);
    }

    @Test
    void duplicateAttributeNameIsRejectedWithinProduct() {
        ProductAttributeRequest request = new ProductAttributeRequest();
        request.setProductId(7L);
        request.setName("口味");
        when(attributeMapper.selectCount(any())).thenReturn(1L);

        assertThrows(CustomValidationException.class, () -> service.saveAttribute(request));
        verify(attributeMapper, never()).insert(any(ProductAttribute.class));
    }

    @Test
    void deletingUsedValueRemovesAffectedSkuAndAllItsMappings() {
        ProductAttributeValue value = value(21L, 7L, 11L, "原味", 1);
        SkuAttributeValue mapping = new SkuAttributeValue();
        mapping.setId(31L);
        mapping.setProductId(7L);
        mapping.setSkuId(70L);
        mapping.setAttributeId(11L);
        mapping.setAttributeValueId(21L);
        when(valueMapper.selectById(21L)).thenReturn(value);
        when(mappingMapper.selectList(any())).thenReturn(List.of(mapping));
        when(valueMapper.deleteById(21L)).thenReturn(1);
        when(skuMapper.deleteByIds(List.of(70L))).thenReturn(1);

        service.deleteValue(7L, 21L);

        verify(mappingMapper).delete(any());
        verify(skuMapper).deleteByIds(List.of(70L));
        verify(valueMapper).deleteById(21L);
    }

    private ProductAttribute attribute(Long id, Long productId, String name, int sort) {
        ProductAttribute attribute = new ProductAttribute();
        attribute.setId(id);
        attribute.setProductId(productId);
        attribute.setName(name);
        attribute.setSort(sort);
        return attribute;
    }

    private ProductAttributeValue value(Long id, Long productId, Long attributeId, String value, int sort) {
        ProductAttributeValue item = new ProductAttributeValue();
        item.setId(id);
        item.setProductId(productId);
        item.setAttributeId(attributeId);
        item.setValue(value);
        item.setSort(sort);
        return item;
    }
}
