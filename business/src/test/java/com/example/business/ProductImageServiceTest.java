package com.example.business;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.business.entity.Product;
import com.example.business.entity.ProductImage;
import com.example.business.entity.ProductSku;
import com.example.business.mapper.ProductImageMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.response.ProductImageResponse;
import com.example.business.service.ValidatedOssStorage;
import com.example.business.service.impl.ProductImageServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {
    @Mock
    private ProductImageMapper imageMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductSkuMapper skuMapper;
    @Mock
    private ValidatedOssStorage storage;

    private ProductImageServiceImpl service;

    @BeforeAll
    static void initializeMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "product-image-test");
        TableInfoHelper.initTableInfo(assistant, ProductImage.class);
        TableInfoHelper.initTableInfo(assistant, Product.class);
        TableInfoHelper.initTableInfo(assistant, ProductSku.class);
    }

    @BeforeEach
    void setUp() {
        service = new ProductImageServiceImpl(imageMapper, productMapper, skuMapper, storage);
    }

    @Test
    void uploadCreatesProductImageAfterValidatedOssUpload() {
        Product product = new Product();
        product.setId(7L);
        when(productMapper.selectById(7L)).thenReturn(product);
        when(storage.storeProductImage(any(), any(), any(), any()))
                .thenReturn("https://bucket.oss.example.com/7/image/image.png");
        when(imageMapper.insert(any(ProductImage.class))).thenAnswer(invocation -> {
            invocation.<ProductImage>getArgument(0).setId(9L);
            return 1;
        });

        ProductImage result = service.upload(7L, null, 3,
                new MockMultipartFile("file", "apple.png", "image/png", pngBytes()));

        assertEquals(9L, result.getId());
        assertEquals(ProductImage.StyleStatus.NONE, result.getStyleStatus());
        assertEquals("https://bucket.oss.example.com/7/image/image.png", result.getOriginalUrl());
        verify(storage).storeProductImage(any(), any(), any(), any());
    }

    @Test
    void setMainClearsSiblingAndBackfillsSkuMainImageId() {
        ProductImage image = image(9L, 7L, 70L);
        ProductSku sku = new ProductSku();
        sku.setId(70L);
        sku.setProductId(7L);
        when(imageMapper.selectById(9L)).thenReturn(image);
        when(skuMapper.selectById(70L)).thenReturn(sku);
        when(imageMapper.update(isNull(), any())).thenReturn(1);
        when(imageMapper.updateById(any(ProductImage.class))).thenReturn(1);
        when(skuMapper.updateById(any(ProductSku.class))).thenReturn(1);

        ProductImageResponse response = service.setMain(9L);

        assertEquals(true, response.getIsMain());
        assertEquals(9L, sku.getMainImageId());
        verify(skuMapper).updateById(sku);
    }

    private static ProductImage image(Long id, Long productId, Long skuId) {
        ProductImage image = new ProductImage();
        image.setId(id);
        image.setProductId(productId);
        image.setSkuId(skuId);
        image.setOriginalUrl("https://bucket.oss.example.com/image.png");
        image.setIsMain(false);
        return image;
    }

    private static byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0};
    }
}
