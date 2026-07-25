package com.example.business;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.business.entity.Category;
import com.example.business.entity.Product;
import com.example.business.entity.ProductSku;
import com.example.business.mapper.CategoryMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.request.ProductPublicListRequest;
import com.example.business.response.ProductDetailResponse;
import com.example.business.response.ProductResponse;
import com.example.business.service.impl.ProductServiceImpl;
import com.example.common.exception.CustomValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductPublicServiceTest {
    @Mock
    private ProductMapper productMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ProductSkuMapper productSkuMapper;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new Configuration(), ProductMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, Product.class);
        productService = new ProductServiceImpl(productMapper, categoryMapper, productSkuMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicListUsesOnSaleFilterAndMinimumSkuPriceAscendingOrder() {
        Product first = product(1L, "苹果", 1, "12.00");
        Product second = product(2L, "梨", 1, "18.00");
        when(productMapper.selectPage(any(), any()))
                .thenAnswer(invocation -> {
                    Page<Product> page = invocation.getArgument(0);
                    page.setRecords(List.of(first, second));
                    page.setTotal(2);
                    return page;
                });
        ProductPublicListRequest request = new ProductPublicListRequest();
        request.setSort("priceAsc");

        var result = productService.getPublicProductPage(request);

        assertEquals(2L, result.getTotal());
        assertEquals(List.of("苹果", "梨"), result.getList().stream()
                .map(response -> response.getTitle())
                .toList());
        ArgumentCaptor<LambdaQueryWrapper<Product>> queryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        org.mockito.Mockito.verify(productMapper).selectPage(any(), queryCaptor.capture());
        assertTrue(!queryCaptor.getValue().getExpression().getNormal().isEmpty());
        String sql = queryCaptor.getValue().getSqlSegment();
        assertTrue(sql.contains("MIN(ps.price)"));
        assertTrue(sql.contains("ps.deleted = 0"));
        assertTrue(sql.contains("ASC"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicListUsesMinimumSkuPriceDescendingOrder() {
        when(productMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProductPublicListRequest request = new ProductPublicListRequest();
        request.setSort("priceDesc");

        productService.getPublicProductPage(request);

        ArgumentCaptor<LambdaQueryWrapper<Product>> queryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        org.mockito.Mockito.verify(productMapper).selectPage(any(), queryCaptor.capture());
        String sql = queryCaptor.getValue().getSqlSegment();
        assertTrue(sql.contains("MIN(ps.price)"));
        assertTrue(sql.contains("ps.deleted = 0"));
        assertTrue(sql.contains("DESC"));
    }

    @Test
    void publicListRejectsUnknownSort() {
        ProductPublicListRequest request = new ProductPublicListRequest();
        request.setSort("random");

        assertThrows(CustomValidationException.class,
                () -> productService.getPublicProductPage(request));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicListSupportsStableRatingSort() {
        when(productMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProductPublicListRequest request = new ProductPublicListRequest();
        request.setSort("rating");

        productService.getPublicProductPage(request);

        ArgumentCaptor<LambdaQueryWrapper<Product>> queryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        org.mockito.Mockito.verify(productMapper).selectPage(any(), queryCaptor.capture());
        assertTrue(queryCaptor.getValue().getSqlSegment().contains("review_count > 0"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicListFiltersByMinimumSkuPriceAndStock() {
        when(productMapper.selectPage(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProductPublicListRequest request = new ProductPublicListRequest();
        request.setMinPrice(new BigDecimal("10.00"));
        request.setMaxPrice(new BigDecimal("50.00"));
        request.setInStock(true);

        productService.getPublicProductPage(request);

        ArgumentCaptor<LambdaQueryWrapper<Product>> queryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        org.mockito.Mockito.verify(productMapper).selectPage(any(), queryCaptor.capture());
        String sql = queryCaptor.getValue().getSqlSegment();
        assertTrue(sql.contains("MIN(ps.price)"));
        assertTrue(sql.contains("ps.stock > 0"));
    }

    @Test
    void publicListRejectsReversedPriceRange() {
        ProductPublicListRequest request = new ProductPublicListRequest();
        request.setMinPrice(new BigDecimal("50.00"));
        request.setMaxPrice(new BigDecimal("10.00"));

        assertThrows(CustomValidationException.class,
                () -> productService.getPublicProductPage(request));
    }

    @Test
    void suggestionsReturnTitlesBeforeEnabledCategoryNamesAndRespectLimit() {
        Product lamp = product(1L, "月光台灯", 1, "88.00");
        lamp.setSoldCount(20);
        Category category = new Category();
        category.setName("灯具");
        category.setEnabled(1);
        when(productMapper.selectList(any())).thenReturn(List.of(lamp));
        when(categoryMapper.selectList(any())).thenReturn(List.of(category));

        List<String> suggestions = productService.getSearchSuggestions("灯", 10);

        assertEquals(List.of("月光台灯", "灯具"), suggestions);
    }

    @Test
    void suggestionsSkipBlankKeywords() {
        assertTrue(productService.getSearchSuggestions("  ", 10).isEmpty());
        org.mockito.Mockito.verifyNoInteractions(productMapper, categoryMapper);
    }

    @Test
    void recommendationsPreferSameCategoryThenFillWithHotProducts() {
        Product current = product(1L, "当前商品", 1, "10.00");
        current.setCategoryId(8L);
        Product sameCategory = product(2L, "同类热销", 1, "20.00");
        sameCategory.setCategoryId(8L);
        Product hotFallback = product(3L, "全站热销", 1, "30.00");
        hotFallback.setCategoryId(9L);
        when(productMapper.selectOne(any(), any(Boolean.class))).thenReturn(current);
        when(productMapper.selectList(any()))
                .thenReturn(List.of(sameCategory))
                .thenReturn(List.of(hotFallback));

        List<ProductResponse> result = productService.getRecommendations(1L, 2);

        assertEquals(List.of(2L, 3L), result.stream().map(ProductResponse::getId).toList());
    }

    @Test
    void recommendationsRejectUnavailableSourceProduct() {
        when(productMapper.selectOne(any(), any(Boolean.class))).thenReturn(null);

        assertThrows(NoSuchElementException.class,
                () -> productService.getRecommendations(99L, 8));
    }

    @Test
    void unavailableProductReturnsNotFoundSignal() {
        when(productMapper.selectOne(any(), any(Boolean.class))).thenReturn(null);

        assertThrows(NoSuchElementException.class,
                () -> productService.getPublicProductDetail(99L));
    }

    @Test
    void detailContainsFlatSkusAndMainImage() {
        Product product = product(7L, "像素苹果", 1, "20.00");
        product.setImage("apple.png");
        ProductSku sku = new ProductSku();
        sku.setId(70L);
        sku.setProductId(7L);
        sku.setPrice(new BigDecimal("21.00"));
        sku.setStock(8);
        when(productMapper.selectOne(any(), any(Boolean.class))).thenReturn(product);
        when(productSkuMapper.selectList(any())).thenReturn(List.of(sku));

        ProductDetailResponse detail = productService.getPublicProductDetail(7L);

        assertEquals(7L, detail.getProduct().getId());
        assertEquals(1, detail.getSkus().size());
        assertEquals(new BigDecimal("21.00"), detail.getSkus().getFirst().getPrice());
        assertTrue(detail.getSkus().getFirst().getAttributeValueIds().isEmpty());
        assertEquals("apple.png", detail.getImages().getFirst().getOriginalUrl());
        assertTrue(detail.getAttributes().isEmpty());
    }

    private Product product(Long id, String title, Integer onSale, String price) {
        Product product = new Product();
        product.setId(id);
        product.setTitle(title);
        product.setOnSale(onSale);
        product.setPrice(new BigDecimal(price));
        return product;
    }
}
