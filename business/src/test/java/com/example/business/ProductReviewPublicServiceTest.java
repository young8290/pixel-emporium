package com.example.business;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.business.entity.OrderItem;
import com.example.business.entity.Product;
import com.example.business.entity.ProductReview;
import com.example.business.enums.ProductReviewStatus;
import com.example.business.mapper.OrderItemMapper;
import com.example.business.mapper.OrderMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductReviewMapper;
import com.example.business.model.ProductReviewAggregate;
import com.example.business.request.ProductReviewPublicListRequest;
import com.example.business.response.ProductReviewResponse;
import com.example.business.response.ProductReviewSummaryResponse;
import com.example.business.service.impl.ProductReviewServiceImpl;
import com.example.common.exception.CustomValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewPublicServiceTest {
    @Mock private ProductReviewMapper reviewMapper;
    @Mock private ProductMapper productMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;

    private ProductReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductReviewServiceImpl(reviewMapper, productMapper, orderMapper, orderItemMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicListReturnsOnlyPublicShapeWithVerifiedPurchaseLabel() throws Exception {
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(onSaleProduct(301L));
        ProductReview review = review(88L, 301L, 701L, 11L, 5, "真实购买评价");
        Page<ProductReview> selected = Page.of(1, 10);
        selected.setTotal(1);
        selected.setRecords(List.of(review));
        when(reviewMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(selected);
        OrderItem item = new OrderItem();
        item.setId(701L);
        item.setSkuTitle("森林绿 / 87键");
        when(orderItemMapper.selectByIds(List.of(701L))).thenReturn(List.of(item));

        ProductReviewPublicListRequest request = new ProductReviewPublicListRequest();
        request.setSize(10);
        var page = service.publicList(301L, request);

        ProductReviewResponse response = page.getList().getFirst();
        assertEquals("已购用户", response.getReviewerLabel());
        assertTrue(response.getVerifiedPurchase());
        assertEquals("森林绿 / 87键", response.getSkuTitle());
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response);
        assertFalse(json.contains("userId"));
        assertFalse(json.contains("orderId"));
        assertFalse(json.contains("orderItemId"));
    }

    @Test
    void summaryReturnsAverageCountAndFiveStarDistribution() {
        when(productMapper.selectOne(any(Wrapper.class))).thenReturn(onSaleProduct(301L));
        when(reviewMapper.selectAggregate(301L)).thenReturn(new ProductReviewAggregate(
                3L, new BigDecimal("4.67"), 0L, 0L, 0L, 1L, 2L));

        ProductReviewSummaryResponse summary = service.summary(301L);

        assertEquals(new BigDecimal("4.67"), summary.getAverageRating());
        assertEquals(3L, summary.getReviewCount());
        assertEquals(2L, summary.getDistribution().get(5));
        assertEquals(0L, summary.getDistribution().get(1));
    }

    @Test
    void publicListRejectsUnsupportedFiltersAndOversizedPages() {
        ProductReviewPublicListRequest badSort = new ProductReviewPublicListRequest();
        badSort.setSort("random");
        assertThrows(CustomValidationException.class, () -> service.publicList(301L, badSort));

        ProductReviewPublicListRequest badRating = new ProductReviewPublicListRequest();
        badRating.setRating(6);
        assertThrows(CustomValidationException.class, () -> service.publicList(301L, badRating));

        ProductReviewPublicListRequest oversized = new ProductReviewPublicListRequest();
        oversized.setSize(51);
        assertThrows(CustomValidationException.class, () -> service.publicList(301L, oversized));
    }

    private Product onSaleProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setOnSale(1);
        return product;
    }

    private ProductReview review(Long id, Long productId, Long itemId, Long userId,
                                 int rating, String content) {
        ProductReview review = new ProductReview();
        review.setId(id);
        review.setProductId(productId);
        review.setOrderId(501L);
        review.setOrderItemId(itemId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setContent(content);
        review.setStatus(ProductReviewStatus.PUBLISHED);
        review.setCreatedAt(LocalDateTime.of(2026, 7, 24, 14, 30));
        review.setUpdatedAt(LocalDateTime.of(2026, 7, 24, 14, 31));
        return review;
    }
}
