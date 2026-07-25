package com.example.business;

import com.example.business.entity.Product;
import com.example.business.entity.ProductAttribute;
import com.example.business.entity.ProductAttributeValue;
import com.example.business.entity.ProductSku;
import com.example.business.entity.SkuAttributeValue;
import com.example.business.mapper.ProductAttributeMapper;
import com.example.business.mapper.ProductAttributeValueMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.mapper.SkuAttributeValueMapper;
import com.example.business.request.ProductSkuAttributeRequest;
import com.example.business.request.ProductSkuGenerateRequest;
import com.example.business.request.ProductSkuRequest;
import com.example.business.service.StockLogService;
import com.example.business.service.impl.ProductSkuServiceImpl;
import com.example.common.exception.CustomValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkuCombinationServiceTest {
    @Mock
    private ProductSkuMapper skuMapper;

    @Mock
    private ProductAttributeMapper attributeMapper;

    @Mock
    private ProductAttributeValueMapper valueMapper;

    @Mock
    private SkuAttributeValueMapper mappingMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private StockLogService stockLogService;

    private ProductSkuServiceImpl service;
    private final List<SkuAttributeValue> mappings = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new ProductSkuServiceImpl(
                skuMapper, attributeMapper, valueMapper, mappingMapper, productMapper, stockLogService);
        lenient().when(productMapper.selectById(7L)).thenReturn(product(7L));
        lenient().when(attributeMapper.selectList(any())).thenReturn(List.of(
                attribute(11L, 7L, "口味", 1),
                attribute(12L, 7L, "重量", 2)));
        lenient().when(valueMapper.selectList(any())).thenReturn(List.of(
                value(101L, 7L, 11L, "原味"),
                value(102L, 7L, 11L, "草莓"),
                value(103L, 7L, 11L, "巧克力"),
                value(201L, 7L, 12L, "250g"),
                value(202L, 7L, 12L, "500g")));
        lenient().when(mappingMapper.selectList(any())).thenAnswer(invocation -> new ArrayList<>(mappings));
        lenient().when(skuMapper.insert(any(ProductSku.class))).thenAnswer(invocation -> {
            ProductSku sku = invocation.getArgument(0);
            sku.setId(1000L + mappings.size());
            return 1;
        });
        lenient().when(mappingMapper.insert(any(SkuAttributeValue.class))).thenAnswer(invocation -> {
            mappings.add(invocation.getArgument(0));
            return 1;
        });
    }

    @Test
    void generatesSixCartesianSkusAndSecondRunIsIdempotent() {
        ProductSkuGenerateRequest request = request(
                selection(11L, 101L, 102L, 103L),
                selection(12L, 201L, 202L));

        var first = service.generate(request);
        var second = service.generate(request);

        assertEquals(6, first.getGeneratedCount());
        assertEquals(0, second.getGeneratedCount());
        assertEquals(6, mappings.stream().map(SkuAttributeValue::getSkuId).distinct().count());
    }

    @Test
    void rejectsCartesianProductOverTwoHundred() {
        ProductSkuGenerateRequest request = new ProductSkuGenerateRequest();
        request.setProductId(7L);
        request.setDefaultPrice(new BigDecimal("12.00"));
        request.setDefaultStock(10);
        request.setSelections(List.of(
                selection(11L, java.util.stream.LongStream.range(1, 202).boxed().toArray(Long[]::new)),
                selection(12L, 201L)));

        CustomValidationException error = assertThrows(CustomValidationException.class,
                () -> service.generate(request));
        assertEquals("SKU组合数量不能超过200", error.getMessage());
    }

    @Test
    void rejectsMissingAttributeDimension() {
        ProductSkuGenerateRequest request = request(selection(11L, 101L, 102L, 103L));

        assertThrows(CustomValidationException.class, () -> service.generate(request));
    }

    @Test
    void rejectsForeignAttributeValue() {
        when(valueMapper.selectList(any())).thenReturn(List.of(value(999L, 8L, 11L, "外部")));
        ProductSkuGenerateRequest request = request(
                selection(11L, 999L), selection(12L, 201L));

        assertThrows(CustomValidationException.class, () -> service.generate(request));
    }

    @Test
    void manualDuplicateCombinationIsRejectedAndNewSkuStoresMappingFields() {
        ProductSkuRequest request = new ProductSkuRequest();
        request.setProductId(7L);
        request.setTitle("原味 250g");
        request.setPrice(new BigDecimal("12.00"));
        request.setStock(9);
        request.setSkuCode("APPLE-ORIGINAL-250");
        request.setMainImageId(88L);
        request.setAttributeValueIds(List.of(101L, 201L));

        service.saveSku(request);
        assertEquals("APPLE-ORIGINAL-250", request.getSkuCode());
        verify(mappingMapper, times(2)).insert(any(SkuAttributeValue.class));

        assertThrows(CustomValidationException.class, () -> service.saveSku(request));
    }

    @Test
    void updatingSkuChangesExistingMappingRowsWithoutViolatingUniqueKey() {
        ProductSku existingSku = new ProductSku();
        existingSku.setId(88L);
        existingSku.setProductId(7L);
        existingSku.setPrice(new BigDecimal("12.00"));
        existingSku.setStock(9);
        existingSku.setTitle("原味 250g");
        SkuAttributeValue existingColor = mapping(1L, 88L, 11L, 101L);
        SkuAttributeValue existingSize = mapping(2L, 88L, 12L, 201L);
        mappings.add(existingColor);
        mappings.add(existingSize);
        when(skuMapper.selectById(88L)).thenReturn(existingSku);
        when(skuMapper.updateById(any(ProductSku.class))).thenReturn(1);
        when(mappingMapper.updateById(any(SkuAttributeValue.class))).thenReturn(1);

        ProductSkuRequest request = new ProductSkuRequest();
        request.setId(88L);
        request.setProductId(7L);
        request.setTitle("草莓 500g");
        request.setPrice(new BigDecimal("13.00"));
        request.setStock(10);
        request.setAttributeValueIds(List.of(102L, 202L));

        assertTrue(service.saveSku(request));
        assertEquals(102L, existingColor.getAttributeValueId());
        assertEquals(202L, existingSize.getAttributeValueId());
        verify(mappingMapper, times(2)).updateById(any(SkuAttributeValue.class));
        verify(mappingMapper, never()).delete(any());
    }

    @Test
    void noSpecProductRejectsSecondDefaultSku() {
        when(attributeMapper.selectList(any())).thenReturn(List.of());
        when(valueMapper.selectList(any())).thenReturn(List.of());
        ProductSku existingSku = new ProductSku();
        existingSku.setId(88L);
        existingSku.setProductId(7L);
        when(skuMapper.selectList(any())).thenReturn(List.of(existingSku));

        ProductSkuRequest request = new ProductSkuRequest();
        request.setProductId(7L);
        request.setTitle("默认规格");
        request.setPrice(new BigDecimal("12.00"));
        request.setStock(10);

        assertThrows(CustomValidationException.class, () -> service.saveSku(request));
        verify(skuMapper, never()).insert(any(ProductSku.class));
    }

    private ProductSkuGenerateRequest request(ProductSkuAttributeRequest... selections) {
        ProductSkuGenerateRequest request = new ProductSkuGenerateRequest();
        request.setProductId(7L);
        request.setDefaultPrice(new BigDecimal("12.00"));
        request.setDefaultStock(10);
        request.setSelections(List.of(selections));
        return request;
    }

    private ProductSkuAttributeRequest selection(Long attributeId, Long... valueIds) {
        ProductSkuAttributeRequest selection = new ProductSkuAttributeRequest();
        selection.setAttributeId(attributeId);
        selection.setAttributeValueIds(List.of(valueIds));
        return selection;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }

    private ProductAttribute attribute(Long id, Long productId, String name, int sort) {
        ProductAttribute attribute = new ProductAttribute();
        attribute.setId(id);
        attribute.setProductId(productId);
        attribute.setName(name);
        attribute.setSort(sort);
        return attribute;
    }

    private ProductAttributeValue value(Long id, Long productId, Long attributeId, String text) {
        ProductAttributeValue value = new ProductAttributeValue();
        value.setId(id);
        value.setProductId(productId);
        value.setAttributeId(attributeId);
        value.setValue(text);
        return value;
    }

    private SkuAttributeValue mapping(Long id, Long skuId, Long attributeId, Long valueId) {
        SkuAttributeValue mapping = new SkuAttributeValue();
        mapping.setId(id);
        mapping.setProductId(7L);
        mapping.setSkuId(skuId);
        mapping.setAttributeId(attributeId);
        mapping.setAttributeValueId(valueId);
        return mapping;
    }
}
