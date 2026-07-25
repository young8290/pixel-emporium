package com.example.business;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.business.entity.Category;
import com.example.business.entity.Product;
import com.example.business.mapper.CategoryMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.request.ProductListRequest;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCategoryEnrichmentTest {
    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ProductSkuMapper productSkuMapper;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productMapper, categoryMapper, productSkuMapper);
    }

    @Test
    void productPageLoadsCategoryNamesInOneBatch() {
        Product product = new Product();
        product.setId(1L);
        product.setTitle("苹果");
        product.setPrice(new BigDecimal("12.80"));
        product.setCategoryId(10L);
        when(productMapper.selectPage(any(), any()))
                .thenAnswer(invocation -> {
                    Page<Product> page = invocation.getArgument(0);
                    page.setRecords(List.of(product));
                    page.setTotal(1);
                    return page;
                });

        Category category = new Category();
        category.setId(10L);
        category.setName("水果");
        when(categoryMapper.selectByIds(any())).thenReturn(List.of(category));

        ProductListRequest request = new ProductListRequest();
        request.setCategoryId(10L);
        var result = productService.getProductPage(request);

        assertEquals("水果", result.getList().getFirst().getCategoryName());
        verify(categoryMapper).selectByIds(java.util.Set.of(10L));
    }
}
