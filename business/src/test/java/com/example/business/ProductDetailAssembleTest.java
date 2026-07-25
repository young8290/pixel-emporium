package com.example.business;

import com.example.business.entity.Product;
import com.example.business.entity.ProductAttribute;
import com.example.business.entity.ProductAttributeValue;
import com.example.business.entity.ProductSku;
import com.example.business.entity.ProductImage;
import com.example.business.entity.SkuAttributeValue;
import com.example.business.mapper.CategoryMapper;
import com.example.business.mapper.ProductAttributeMapper;
import com.example.business.mapper.ProductAttributeValueMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.mapper.ProductImageMapper;
import com.example.business.mapper.SkuAttributeValueMapper;
import com.example.business.response.ProductDetailResponse;
import com.example.business.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductDetailAssembleTest {
    @Mock
    private ProductMapper productMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private ProductSkuMapper productSkuMapper;
    @Mock
    private ProductAttributeMapper attributeMapper;
    @Mock
    private ProductAttributeValueMapper valueMapper;
    @Mock
    private SkuAttributeValueMapper mappingMapper;
    @Mock
    private ProductImageMapper imageMapper;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productMapper, categoryMapper, productSkuMapper,
                attributeMapper, valueMapper, mappingMapper, imageMapper);
    }

    @Test
    void publicDetailAssemblesAttributeTreeAndSkuMappings() {
        Product product = new Product();
        product.setId(7L);
        product.setTitle("像素苹果");
        product.setOnSale(1);
        product.setImage("apple.png");
        product.setPrice(new BigDecimal("20.00"));

        ProductAttribute color = new ProductAttribute();
        color.setId(11L);
        color.setProductId(7L);
        color.setName("颜色");
        color.setSort(1);
        ProductAttribute size = new ProductAttribute();
        size.setId(12L);
        size.setProductId(7L);
        size.setName("尺寸");
        size.setSort(2);

        ProductAttributeValue red = new ProductAttributeValue();
        red.setId(101L);
        red.setProductId(7L);
        red.setAttributeId(11L);
        red.setValue("红色");
        ProductAttributeValue large = new ProductAttributeValue();
        large.setId(201L);
        large.setProductId(7L);
        large.setAttributeId(12L);
        large.setValue("大");

        ProductSku sku = new ProductSku();
        sku.setId(70L);
        sku.setProductId(7L);
        sku.setPrice(new BigDecimal("21.00"));
        sku.setOriginalPrice(new BigDecimal("25.00"));
        sku.setStock(8);
        sku.setSkuCode("APPLE-RED-L");
        sku.setMainImageId(9001L);

        SkuAttributeValue colorMapping = new SkuAttributeValue();
        colorMapping.setProductId(7L);
        colorMapping.setSkuId(70L);
        colorMapping.setAttributeId(11L);
        colorMapping.setAttributeValueId(101L);
        SkuAttributeValue sizeMapping = new SkuAttributeValue();
        sizeMapping.setProductId(7L);
        sizeMapping.setSkuId(70L);
        sizeMapping.setAttributeId(12L);
        sizeMapping.setAttributeValueId(201L);

        ProductImage styledImage = new ProductImage();
        styledImage.setId(9001L);
        styledImage.setProductId(7L);
        styledImage.setOriginalUrl("apple.png");
        styledImage.setStyledUrl("apple-styled.png");
        styledImage.setStyleStatus(ProductImage.StyleStatus.DONE);
        styledImage.setIsMain(true);

        when(productMapper.selectOne(any(), any(Boolean.class))).thenReturn(product);
        when(productSkuMapper.selectList(any())).thenReturn(List.of(sku));
        when(attributeMapper.selectList(any())).thenReturn(List.of(color, size));
        when(valueMapper.selectList(any())).thenReturn(List.of(red, large));
        when(mappingMapper.selectList(any())).thenReturn(List.of(colorMapping, sizeMapping));
        when(imageMapper.selectList(any())).thenReturn(List.of(styledImage));

        var detail = productService.getPublicProductDetail(7L);

        assertEquals("像素苹果", detail.getProduct().getTitle());
        assertEquals("apple.png", detail.getImages().getFirst().getOriginalUrl());
        assertEquals("apple-styled.png", detail.getImages().getFirst().getStyledUrl());
        assertEquals("apple-styled.png", detail.getImages().getFirst().getUrl());
        assertEquals(List.of("颜色", "尺寸"), detail.getAttributes().stream()
                .map(ProductDetailResponse.AttributeResponse::getName).toList());
        assertEquals(List.of("红色"), detail.getAttributes().getFirst().getValues().stream()
                .map(ProductDetailResponse.AttributeValueResponse::getValue).toList());
        assertEquals(List.of(101L, 201L), detail.getSkus().getFirst().getAttributeValueIds());
        assertEquals("APPLE-RED-L", detail.getSkus().getFirst().getSkuCode());
        assertEquals(new BigDecimal("25.00"), detail.getSkus().getFirst().getOriginalPrice());
        assertEquals(9001L, detail.getSkus().getFirst().getMainImageId());
    }
}
