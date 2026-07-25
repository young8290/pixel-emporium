package com.example.business;

import com.example.business.entity.ProductSku;
import com.example.business.entity.Product;
import com.example.business.mapper.ProductAttributeMapper;
import com.example.business.mapper.ProductAttributeValueMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.mapper.SkuAttributeValueMapper;
import com.example.business.request.ProductSkuRequest;
import com.example.business.response.ProductDetailResponse;
import com.example.business.response.ProductSkuResponse;
import com.example.business.service.StockLogService;
import com.example.business.service.impl.ProductSkuServiceImpl;
import com.example.common.exception.CustomValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSkuPricingTest {
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

    @Test
    void migrationAndContractsExposeOptionalOriginalPrice() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V16__product_sku_original_price.sql");
        assertTrue(Files.exists(migration));

        String sql = Files.readString(migration).toLowerCase().replaceAll("\\s+", " ");
        assertTrue(sql.contains("add column `original_price` decimal(10, 2) null"));
        assertTrue(sql.contains("check (`original_price` is null or `original_price` > `price`)"));

        assertEquals(BigDecimal.class, ProductSku.class.getDeclaredField("originalPrice").getType());
        assertEquals(BigDecimal.class, ProductSkuRequest.class.getDeclaredField("originalPrice").getType());
        assertEquals(BigDecimal.class, ProductSkuResponse.class.getDeclaredField("originalPrice").getType());
        assertEquals(BigDecimal.class,
                ProductDetailResponse.SkuResponse.class.getDeclaredField("originalPrice").getType());
    }

    @Test
    void originalPriceMustBeStrictlyHigherThanSalePrice() {
        ProductSkuRequest request = validRequest();
        request.setPrice(new BigDecimal("10.00"));
        request.setOriginalPrice(new BigDecimal("10.00"));

        CustomValidationException error = assertThrows(CustomValidationException.class,
                () -> service().saveSku(request));

        assertEquals("原价必须高于现价", error.getMessage());
    }

    @Test
    void validPricesAreStoredWithTwoDecimalPlaces() {
        ProductSkuRequest request = validRequest();
        request.setPrice(new BigDecimal("8.5"));
        request.setOriginalPrice(new BigDecimal("12"));
        Product product = new Product();
        product.setId(7L);
        when(productMapper.selectById(7L)).thenReturn(product);
        when(attributeMapper.selectList(any())).thenReturn(java.util.List.of());
        when(valueMapper.selectList(any())).thenReturn(java.util.List.of());
        when(skuMapper.selectList(any())).thenReturn(java.util.List.of());
        when(mappingMapper.selectList(any())).thenReturn(java.util.List.of());
        when(skuMapper.insert(any(ProductSku.class))).thenAnswer(invocation -> {
            ProductSku sku = invocation.getArgument(0);
            sku.setId(71L);
            return 1;
        });

        service().saveSku(request);

        ArgumentCaptor<ProductSku> saved = ArgumentCaptor.forClass(ProductSku.class);
        verify(skuMapper).insert(saved.capture());
        assertEquals(new BigDecimal("8.50"), saved.getValue().getPrice());
        assertEquals(2, saved.getValue().getPrice().scale());
        assertEquals(new BigDecimal("12.00"), saved.getValue().getOriginalPrice());
        assertEquals(2, saved.getValue().getOriginalPrice().scale());
    }

    private ProductSkuRequest validRequest() {
        ProductSkuRequest request = new ProductSkuRequest();
        request.setProductId(7L);
        request.setTitle("默认规格");
        request.setPrice(new BigDecimal("8.50"));
        request.setStock(10);
        return request;
    }

    private ProductSkuServiceImpl service() {
        return new ProductSkuServiceImpl(skuMapper, attributeMapper, valueMapper,
                mappingMapper, productMapper, stockLogService);
    }
}
