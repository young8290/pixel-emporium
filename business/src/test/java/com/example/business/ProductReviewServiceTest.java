package com.example.business;

import com.example.business.entity.Order;
import com.example.business.entity.OrderItem;
import com.example.business.entity.Product;
import com.example.business.entity.ProductReview;
import com.example.business.enums.OrderStatus;
import com.example.business.enums.ProductReviewStatus;
import com.example.business.mapper.OrderItemMapper;
import com.example.business.mapper.OrderMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductReviewMapper;
import com.example.business.model.ProductReviewAggregate;
import com.example.business.request.ProductReviewCreateRequest;
import com.example.business.request.ProductReviewUpdateRequest;
import com.example.business.response.OrderReviewEligibilityResponse;
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
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {
    @Mock private ProductReviewMapper reviewMapper;
    @Mock private ProductMapper productMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;

    private ProductReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductReviewServiceImpl(reviewMapper, productMapper, orderMapper, orderItemMapper);
        UserContext.set(11L);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void completedOwnedOrderCreatesPublishedReviewAndRecalculatesProduct() {
        OrderItem item = item(701L, 501L, 301L);
        when(orderItemMapper.selectById(701L)).thenReturn(item);
        when(orderMapper.selectById(501L)).thenReturn(order(501L, 11L, OrderStatus.COMPLETED));
        when(productMapper.selectByIdForUpdate(301L)).thenReturn(product(301L));
        when(reviewMapper.insert(any(ProductReview.class))).thenReturn(1);
        when(reviewMapper.selectAggregate(301L))
                .thenReturn(new ProductReviewAggregate(1L, new BigDecimal("5.00")));
        when(productMapper.updateReviewAggregate(301L, new BigDecimal("5.00"), 1L)).thenReturn(1);

        ProductReview review = service.create(create(701L, 5, "  很好用的商品  "));

        assertEquals(11L, review.getUserId());
        assertEquals(301L, review.getProductId());
        assertEquals("很好用的商品", review.getContent());
        assertEquals(ProductReviewStatus.PUBLISHED, review.getStatus());
        InOrder order = inOrder(productMapper, reviewMapper);
        order.verify(productMapper).selectByIdForUpdate(301L);
        order.verify(reviewMapper).insert(review);
        order.verify(reviewMapper).selectAggregate(301L);
        order.verify(productMapper).updateReviewAggregate(301L, new BigDecimal("5.00"), 1L);
    }

    @Test
    void nonCompletedOrForeignOrderCannotBeReviewed() {
        when(orderItemMapper.selectById(701L)).thenReturn(item(701L, 501L, 301L));
        when(orderMapper.selectById(501L)).thenReturn(order(501L, 99L, OrderStatus.COMPLETED));
        assertThrows(java.util.NoSuchElementException.class,
                () -> service.create(create(701L, 5, "这是有效评论")));

        when(orderMapper.selectById(501L)).thenReturn(order(501L, 11L, OrderStatus.SHIPPED));
        assertThrows(CustomConflictException.class,
                () -> service.create(create(701L, 5, "这是有效评论")));
        verify(productMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void serviceValidatesTrimmedContentAndRating() {
        assertThrows(CustomValidationException.class,
                () -> service.create(create(701L, 0, "这是有效评论")));
        assertThrows(CustomValidationException.class,
                () -> service.create(create(701L, 5, "  四字  ")));
        assertThrows(CustomValidationException.class,
                () -> service.update(88L, update(5, "x".repeat(1001))));
    }

    @Test
    void duplicateOrderItemIsReportedAsConflict() {
        when(orderItemMapper.selectById(701L)).thenReturn(item(701L, 501L, 301L));
        when(orderMapper.selectById(501L)).thenReturn(order(501L, 11L, OrderStatus.COMPLETED));
        when(productMapper.selectByIdForUpdate(301L)).thenReturn(product(301L));
        when(reviewMapper.insert(any(ProductReview.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        assertThrows(CustomConflictException.class,
                () -> service.create(create(701L, 5, "这是有效评论")));
        verify(reviewMapper, never()).selectAggregate(any());
    }

    @Test
    void hiddenReviewCannotBeEditedButCanBeDeletedByOwner() {
        ProductReview hidden = review(88L, 301L, 11L, ProductReviewStatus.HIDDEN);
        when(reviewMapper.selectOwnedActiveById(88L, 11L)).thenReturn(hidden);

        assertThrows(CustomConflictException.class,
                () -> service.update(88L, update(4, "修改后的评论")));

        when(productMapper.selectByIdForUpdate(301L)).thenReturn(product(301L));
        when(reviewMapper.softDeleteByOwner(88L, 11L)).thenReturn(1);
        when(reviewMapper.selectAggregate(301L))
                .thenReturn(new ProductReviewAggregate(0L, new BigDecimal("0.00")));
        when(productMapper.updateReviewAggregate(301L, new BigDecimal("0.00"), 0L)).thenReturn(1);

        assertTrue(service.delete(88L));
        verify(reviewMapper).softDeleteByOwner(88L, 11L);
    }

    @Test
    void completedOrderEligibilitySeparatesUnreviewedPublishedAndHiddenItems() {
        when(orderMapper.selectById(501L)).thenReturn(order(501L, 11L, OrderStatus.COMPLETED));
        OrderItem first = item(701L, 501L, 301L);
        first.setProductTitle("像素键盘");
        first.setSkuTitle("森林绿");
        OrderItem second = item(702L, 501L, 302L);
        second.setProductTitle("像素鼠标");
        second.setSkuTitle("米白");
        OrderItem third = item(703L, 501L, 303L);
        third.setProductTitle("像素耳机");
        third.setSkuTitle("黑色");
        when(orderItemMapper.selectList(any())).thenReturn(java.util.List.of(first, second, third));

        ProductReview published = review(801L, 301L, 11L, ProductReviewStatus.PUBLISHED);
        published.setOrderItemId(701L);
        published.setRating(5);
        published.setContent("已经购买并使用，体验很好");
        ProductReview hidden = review(802L, 302L, 11L, ProductReviewStatus.HIDDEN);
        hidden.setOrderItemId(702L);
        hidden.setRating(2);
        hidden.setContent("已经购买并使用，体验一般");
        hidden.setModerationReason("包含不当内容");
        when(reviewMapper.selectList(any())).thenReturn(java.util.List.of(published, hidden));

        OrderReviewEligibilityResponse result = service.orderEligibility(501L);

        assertEquals(3, result.getItems().size());
        var unreviewed = result.getItems().get(2);
        assertTrue(unreviewed.getEligible());
        assertFalse(unreviewed.getEditable());
        assertFalse(unreviewed.getDeletable());
        assertEquals("303", String.valueOf(unreviewed.getProductId()));
        var publishedItem = result.getItems().get(0);
        assertTrue(publishedItem.getEditable());
        assertTrue(publishedItem.getDeletable());
        assertEquals(5, publishedItem.getRating());
        var hiddenItem = result.getItems().get(1);
        assertFalse(hiddenItem.getEditable());
        assertTrue(hiddenItem.getDeletable());
        assertEquals("包含不当内容", hiddenItem.getModerationReason());
    }

    @Test
    void foreignOrderCannotRevealReviewEligibility() {
        when(orderMapper.selectById(501L)).thenReturn(order(501L, 99L, OrderStatus.COMPLETED));

        assertThrows(java.util.NoSuchElementException.class,
                () -> service.orderEligibility(501L));
        verify(orderItemMapper, never()).selectList(any());
    }

    private ProductReviewCreateRequest create(Long itemId, int rating, String content) {
        ProductReviewCreateRequest request = new ProductReviewCreateRequest();
        request.setOrderItemId(itemId);
        request.setRating(rating);
        request.setContent(content);
        return request;
    }

    private ProductReviewUpdateRequest update(int rating, String content) {
        ProductReviewUpdateRequest request = new ProductReviewUpdateRequest();
        request.setRating(rating);
        request.setContent(content);
        return request;
    }

    private OrderItem item(Long id, Long orderId, Long productId) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrderId(orderId);
        item.setProductId(productId);
        return item;
    }

    private Order order(Long id, Long userId, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(status);
        return order;
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }

    private ProductReview review(Long id, Long productId, Long userId, ProductReviewStatus status) {
        ProductReview review = new ProductReview();
        review.setId(id);
        review.setProductId(productId);
        review.setUserId(userId);
        review.setStatus(status);
        return review;
    }
}
