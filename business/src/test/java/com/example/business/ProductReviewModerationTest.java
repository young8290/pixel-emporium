package com.example.business;

import com.example.business.entity.Product;
import com.example.business.entity.ProductReview;
import com.example.business.enums.ProductReviewStatus;
import com.example.business.mapper.OrderItemMapper;
import com.example.business.mapper.OrderMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductReviewMapper;
import com.example.business.model.ProductReviewAggregate;
import com.example.business.request.ProductReviewModerateRequest;
import com.example.business.service.impl.ProductReviewServiceImpl;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewModerationTest {
    @Mock private ProductReviewMapper reviewMapper;
    @Mock private ProductMapper productMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;

    private ProductReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductReviewServiceImpl(reviewMapper, productMapper, orderMapper, orderItemMapper);
        UserContext.set(99L);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void hideAndRestoreRecalculatePublishedAggregate() {
        ProductReview published = review(88L, 301L, ProductReviewStatus.PUBLISHED);
        when(reviewMapper.selectById(88L)).thenReturn(published);
        when(productMapper.selectByIdForUpdate(301L)).thenReturn(product(301L));
        when(reviewMapper.hidePublished(88L, 99L, "内容不符合规范")).thenReturn(1);
        when(reviewMapper.selectAggregate(301L))
                .thenReturn(new ProductReviewAggregate(0L, new BigDecimal("0.00")));
        when(productMapper.updateReviewAggregate(301L, new BigDecimal("0.00"), 0L)).thenReturn(1);

        assertTrue(service.hide(88L, moderate("  内容不符合规范  ")));

        InOrder hideOrder = inOrder(productMapper, reviewMapper);
        hideOrder.verify(productMapper).selectByIdForUpdate(301L);
        hideOrder.verify(reviewMapper).hidePublished(88L, 99L, "内容不符合规范");
        hideOrder.verify(reviewMapper).selectAggregate(301L);

        ProductReview hidden = review(89L, 302L, ProductReviewStatus.HIDDEN);
        when(reviewMapper.selectById(89L)).thenReturn(hidden);
        when(productMapper.selectByIdForUpdate(302L)).thenReturn(product(302L));
        when(reviewMapper.restoreHidden(89L, 99L)).thenReturn(1);
        when(reviewMapper.selectAggregate(302L))
                .thenReturn(new ProductReviewAggregate(1L, new BigDecimal("4.00")));
        when(productMapper.updateReviewAggregate(302L, new BigDecimal("4.00"), 1L)).thenReturn(1);

        assertTrue(service.restore(89L));
        verify(reviewMapper).restoreHidden(89L, 99L);
    }

    @Test
    void repeatedModerationAndShortTrimmedReasonAreRejected() {
        when(reviewMapper.selectById(88L))
                .thenReturn(review(88L, 301L, ProductReviewStatus.HIDDEN));
        assertThrows(CustomConflictException.class,
                () -> service.hide(88L, moderate("重复隐藏")));

        when(reviewMapper.selectById(89L))
                .thenReturn(review(89L, 302L, ProductReviewStatus.PUBLISHED));
        assertThrows(CustomConflictException.class, () -> service.restore(89L));
        assertThrows(CustomValidationException.class,
                () -> service.hide(90L, moderate(" a ")));
        verify(productMapper, never()).selectByIdForUpdate(301L);
    }

    private ProductReviewModerateRequest moderate(String reason) {
        ProductReviewModerateRequest request = new ProductReviewModerateRequest();
        request.setReason(reason);
        return request;
    }

    private ProductReview review(Long id, Long productId, ProductReviewStatus status) {
        ProductReview review = new ProductReview();
        review.setId(id);
        review.setProductId(productId);
        review.setStatus(status);
        return review;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }
}
