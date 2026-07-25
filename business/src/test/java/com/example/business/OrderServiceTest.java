package com.example.business;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.business.entity.Cart;
import com.example.business.entity.Order;
import com.example.business.entity.OrderItem;
import com.example.business.entity.Product;
import com.example.business.entity.ProductSku;
import com.example.business.enums.OrderStatus;
import com.example.business.mapper.CartMapper;
import com.example.business.mapper.OrderItemMapper;
import com.example.business.mapper.OrderMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.model.OrderAddressSnapshot;
import com.example.business.request.OrderCreateRequest;
import com.example.business.request.OrderListRequest;
import com.example.business.response.OrderDetailResponse;
import com.example.business.response.OrderResponse;
import com.example.business.service.ProductSkuService;
import com.example.business.service.NotificationOutboxService;
import com.example.business.service.impl.OrderServiceImpl;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomForbiddenException;
import com.example.common.exception.CustomValidationException;
import com.example.common.response.PageResponse;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private CartMapper cartMapper;
    @Mock private ProductSkuMapper productSkuMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ProductSkuService productSkuService;
    @Mock private IdentifierGenerator identifierGenerator;
    @Mock private NotificationOutboxService notificationOutboxService;

    private OrderServiceImpl service;

    @BeforeAll
    static void initializeMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "order-test");
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, OrderItem.class);
    }

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderMapper, orderItemMapper, cartMapper,
                productSkuMapper, productMapper, productSkuService, identifierGenerator);
        ReflectionTestUtils.setField(service, "notificationOutboxService", notificationOutboxService);
        UserContext.set(11L);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void cartOrderUsesServerSnapshotsAndDeductsInSkuOrder() {
        OrderCreateRequest request = cartRequest(List.of(91L, 90L));
        when(orderMapper.selectAddressById(1L)).thenReturn(address(11L));
        when(cartMapper.selectByIds(anyCollection())).thenReturn(List.of(
                cart(90L, 11L, 80L, 1, true),
                cart(91L, 11L, 70L, 2, true)));
        when(productSkuMapper.selectByIds(anyCollection())).thenReturn(List.of(
                sku(80L, 8L, "5.00"),
                sku(70L, 7L, "12.00")));
        when(productMapper.selectByIds(anyCollection())).thenReturn(List.of(
                product(8L, "商品8", "8.png", 1),
                product(7L, "商品7", "7.png", 1)));
        when(identifierGenerator.nextId(any())).thenReturn(1001L);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);
        when(cartMapper.logicalDeleteBatch(eq(11L), any())).thenReturn(2);

        String orderNo = service.create(request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertEquals(orderNo, saved.getOrderNo());
        assertTrue(orderNo.matches("\\d{8}1001"));
        assertEquals(1001L, saved.getId());
        assertEquals(11L, saved.getUserId());
        assertEquals(OrderStatus.PENDING_PAYMENT, saved.getStatus());
        assertEquals(new BigDecimal("29.00"), saved.getTotalAmount());
        assertEquals(new BigDecimal("29.00"), saved.getPayAmount());
        assertEquals(new BigDecimal("0.00"), saved.getFreightAmount());
        assertEquals("张三", saved.getReceiverName());
        assertEquals("广东省", saved.getReceiverProvince());

        InOrder stockOrder = inOrder(productSkuService);
        stockOrder.verify(productSkuService).deductStock(70L, 2, orderNo, 11L);
        stockOrder.verify(productSkuService).deductStock(80L, 1, orderNo, 11L);

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemMapper, times(2)).insert(itemCaptor.capture());
        List<OrderItem> items = itemCaptor.getAllValues();
        assertEquals(List.of(70L, 80L), items.stream().map(OrderItem::getProductSkuId).toList());
        assertEquals("商品7", items.getFirst().getProductTitle());
        assertEquals("SKU70", items.getFirst().getSkuTitle());
        assertEquals("7.png", items.getFirst().getImage());
        assertEquals(new BigDecimal("12.00"), items.getFirst().getPrice());
        assertEquals(2, items.getFirst().getQuantity());
        verify(cartMapper).logicalDeleteBatch(11L, List.of(90L, 91L));
        verify(notificationOutboxService).appendOrderCreated(1001L);
    }

    @Test
    void cartOrderWithoutIdsUsesAllCheckedRows() {
        OrderCreateRequest request = cartRequest(null);
        when(orderMapper.selectAddressById(1L)).thenReturn(address(11L));
        when(cartMapper.selectList(any())).thenReturn(List.of(cart(90L, 11L, 70L, 1, true)));
        stubCatalogAndWrites();
        when(cartMapper.logicalDeleteBatch(11L, List.of(90L))).thenReturn(1);

        service.create(request);

        verify(cartMapper).selectList(any());
        verify(cartMapper).logicalDeleteBatch(11L, List.of(90L));
    }

    @Test
    void buyNowNeverReadsOrDeletesCartRows() {
        OrderCreateRequest request = buyNowRequest();
        when(orderMapper.selectAddressById(1L)).thenReturn(address(11L));
        stubCatalogAndWrites();

        String orderNo = service.create(request);

        verify(cartMapper, never()).selectList(any());
        verify(cartMapper, never()).selectByIds(anyCollection());
        verify(cartMapper, never()).logicalDeleteBatch(any(), any());
        verify(productSkuService).deductStock(70L, 1, orderNo, 11L);
    }

    @Test
    void rejectsMixedSourceFieldsAndDuplicateCartIds() {
        OrderCreateRequest cartWithBuyNowFields = cartRequest(List.of(90L));
        cartWithBuyNowFields.setSkuId(70L);
        cartWithBuyNowFields.setQuantity(1);
        assertThrows(CustomValidationException.class, () -> service.create(cartWithBuyNowFields));

        OrderCreateRequest buyNowWithCart = buyNowRequest();
        buyNowWithCart.setCartItemIds(List.of(90L));
        assertThrows(CustomValidationException.class, () -> service.create(buyNowWithCart));

        OrderCreateRequest duplicates = cartRequest(List.of(90L, 90L));
        assertThrows(CustomValidationException.class, () -> service.create(duplicates));
        verify(orderMapper, never()).selectAddressById(any());
    }

    @Test
    void rejectsMissingForeignAndIncompleteAddress() {
        OrderCreateRequest request = buyNowRequest();
        when(orderMapper.selectAddressById(1L))
                .thenReturn(null)
                .thenReturn(address(12L))
                .thenReturn(address(11L));

        assertThrows(NoSuchElementException.class, () -> service.create(request));
        assertThrows(CustomForbiddenException.class, () -> service.create(request));

        OrderAddressSnapshot incomplete = address(11L);
        incomplete.setPhone(" ");
        when(orderMapper.selectAddressById(1L)).thenReturn(incomplete);
        assertThrows(CustomValidationException.class, () -> service.create(request));
    }

    @Test
    void rejectsForeignMissingAndUncheckedExplicitCartRows() {
        OrderCreateRequest request = cartRequest(List.of(90L, 91L));
        when(orderMapper.selectAddressById(1L)).thenReturn(address(11L));
        when(cartMapper.selectByIds(anyCollection()))
                .thenReturn(List.of(
                        cart(90L, 11L, 70L, 1, true),
                        cart(91L, 12L, 80L, 1, true)))
                .thenReturn(List.of(cart(90L, 11L, 70L, 1, true)))
                .thenReturn(List.of(
                        cart(90L, 11L, 70L, 1, true),
                        cart(91L, 11L, 80L, 1, false)));

        assertThrows(CustomForbiddenException.class, () -> service.create(request));
        assertThrows(CustomValidationException.class, () -> service.create(request));
        assertThrows(CustomValidationException.class, () -> service.create(request));
        verify(productSkuService, never()).deductStock(any(), any(), any(), any());
    }

    @Test
    void rejectsMissingSkuMissingProductOffSaleProductAndNegativePrice() {
        OrderCreateRequest request = buyNowRequest();
        when(orderMapper.selectAddressById(1L)).thenReturn(address(11L));

        when(productSkuMapper.selectByIds(anyCollection())).thenReturn(List.of());
        assertThrows(NoSuchElementException.class, () -> service.create(request));

        when(productSkuMapper.selectByIds(anyCollection())).thenReturn(List.of(sku(70L, 7L, "12.00")));
        when(productMapper.selectByIds(anyCollection())).thenReturn(List.of());
        assertThrows(NoSuchElementException.class, () -> service.create(request));

        when(productMapper.selectByIds(anyCollection()))
                .thenReturn(List.of(product(7L, "商品7", "7.png", 0)));
        assertThrows(CustomValidationException.class, () -> service.create(request));

        when(productMapper.selectByIds(anyCollection()))
                .thenReturn(List.of(product(7L, "商品7", "7.png", 1)));
        when(productSkuMapper.selectByIds(anyCollection())).thenReturn(List.of(sku(70L, 7L, "-0.01")));
        assertThrows(CustomValidationException.class, () -> service.create(request));
        verify(productSkuService, never()).deductStock(any(), any(), any(), any());
    }

    @Test
    void propagatesStockAndPersistenceFailuresForTransactionRollback() {
        OrderCreateRequest request = buyNowRequest();
        when(orderMapper.selectAddressById(1L)).thenReturn(address(11L));
        when(productSkuMapper.selectByIds(anyCollection())).thenReturn(List.of(sku(70L, 7L, "12.00")));
        when(productMapper.selectByIds(anyCollection()))
                .thenReturn(List.of(product(7L, "商品7", "7.png", 1)));
        when(identifierGenerator.nextId(any())).thenReturn(1001L);
        org.mockito.Mockito.doThrow(new CustomBusinessException("库存不足"))
                .when(productSkuService).deductStock(eq(70L), eq(1), anyString(), eq(11L));

        assertThrows(CustomBusinessException.class, () -> service.create(request));
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void rejectsShortCartDeleteAfterWrites() {
        OrderCreateRequest request = cartRequest(List.of(90L));
        when(orderMapper.selectAddressById(1L)).thenReturn(address(11L));
        when(cartMapper.selectByIds(anyCollection()))
                .thenReturn(List.of(cart(90L, 11L, 70L, 1, true)));
        stubCatalogAndWrites();
        when(cartMapper.logicalDeleteBatch(11L, List.of(90L))).thenReturn(0);

        assertThrows(CustomBusinessException.class, () -> service.create(request));
    }

    @Test
    void markPaidLocksAndTransfersOnlyPendingOrder() {
        LocalDateTime paidAt = LocalDateTime.of(2026, 7, 23, 12, 0);
        Order pending = order(501L, 11L, OrderStatus.PENDING_PAYMENT);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(pending);
        when(orderMapper.updateById(pending)).thenReturn(1);
        when(productMapper.incrementSoldCountByOrderId(501L)).thenReturn(2);

        assertTrue(service.markPaid(501L, paidAt));

        InOrder calls = inOrder(orderMapper, productMapper);
        calls.verify(orderMapper).selectByIdForUpdate(501L);
        calls.verify(orderMapper).updateById(pending);
        calls.verify(productMapper).incrementSoldCountByOrderId(501L);
        assertEquals(OrderStatus.PAID, pending.getStatus());
        assertEquals(paidAt, pending.getPaidAt());
    }

    @Test
    void markPaidRejectsMissingNullTimeAndEveryNonPendingState() {
        assertThrows(CustomValidationException.class,
                () -> service.markPaid(501L, null));
        when(orderMapper.selectByIdForUpdate(501L))
                .thenReturn(null)
                .thenReturn(order(501L, 11L, OrderStatus.PAID))
                .thenReturn(order(501L, 11L, OrderStatus.SHIPPED))
                .thenReturn(order(501L, 11L, OrderStatus.COMPLETED))
                .thenReturn(order(501L, 11L, OrderStatus.CANCELED));

        assertThrows(NoSuchElementException.class,
                () -> service.markPaid(501L, LocalDateTime.now()));
        for (int i = 0; i < 4; i++) {
            assertThrows(CustomConflictException.class,
                    () -> service.markPaid(501L, LocalDateTime.now()));
        }
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    void cancelLocksPendingOrderRestoresStockOnceAndPersistsCanceledState() {
        Order pending = order(501L, 11L, OrderStatus.PENDING_PAYMENT);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(pending);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(
                item(2L, 501L, 80L, 1),
                item(1L, 501L, 70L, 2)));
        when(orderMapper.updateById(any(Order.class))).thenReturn(1);

        assertTrue(service.cancel(501L));

        InOrder calls = inOrder(orderMapper, orderItemMapper, productSkuService);
        calls.verify(orderMapper).selectByIdForUpdate(501L);
        calls.verify(orderItemMapper).selectList(any());
        calls.verify(productSkuService).restoreStock(70L, 2, null, 11L);
        calls.verify(productSkuService).restoreStock(80L, 1, null, 11L);
        calls.verify(orderMapper).updateById(pending);
        assertEquals(OrderStatus.CANCELED, pending.getStatus());
        assertTrue(pending.getCanceledAt() != null);
    }

    @Test
    void cancelRejectsMissingForeignAndEveryNonPendingState() {
        when(orderMapper.selectByIdForUpdate(501L))
                .thenReturn(null)
                .thenReturn(order(501L, 12L, OrderStatus.PENDING_PAYMENT))
                .thenReturn(order(501L, 11L, OrderStatus.PAID))
                .thenReturn(order(501L, 11L, OrderStatus.SHIPPED))
                .thenReturn(order(501L, 11L, OrderStatus.COMPLETED))
                .thenReturn(order(501L, 11L, OrderStatus.CANCELED));

        assertThrows(NoSuchElementException.class, () -> service.cancel(501L));
        assertThrows(CustomForbiddenException.class, () -> service.cancel(501L));
        for (int i = 0; i < 4; i++) {
            assertThrows(CustomConflictException.class, () -> service.cancel(501L));
        }
        verify(productSkuService, never()).restoreStock(any(), any(), any(), any());
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    void cancelRejectsCorruptOrderWithoutItemsInsteadOfLosingStockRestoration() {
        Order pending = order(501L, 11L, OrderStatus.PENDING_PAYMENT);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(pending);
        when(orderItemMapper.selectList(any())).thenReturn(List.of());

        assertThrows(NoSuchElementException.class, () -> service.cancel(501L));

        verify(productSkuService, never()).restoreStock(any(), any(), any(), any());
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    void confirmOnlyAllowsOwnedShippedOrderAndNeverTouchesStock() {
        Order shipped = order(501L, 11L, OrderStatus.SHIPPED);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(shipped);
        when(orderMapper.updateById(shipped)).thenReturn(1);

        assertTrue(service.confirm(501L));

        assertEquals(OrderStatus.COMPLETED, shipped.getStatus());
        assertTrue(shipped.getCompletedAt() != null);
        verify(productSkuService, never()).restoreStock(any(), any(), any(), any());

        when(orderMapper.selectByIdForUpdate(502L))
                .thenReturn(order(502L, 11L, OrderStatus.PENDING_PAYMENT));
        assertThrows(CustomConflictException.class, () -> service.confirm(502L));
    }

    @Test
    void timeoutScanIsBoundedRechecksLockedStateAndSkipsChangedRows() {
        when(orderMapper.selectTimeoutIds(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of(501L, 502L, 503L));
        Order pending = order(501L, 11L, OrderStatus.PENDING_PAYMENT);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(pending);
        when(orderMapper.selectByIdForUpdate(502L))
                .thenReturn(order(502L, 11L, OrderStatus.PAID));
        when(orderMapper.selectByIdForUpdate(503L)).thenReturn(null);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item(1L, 501L, 70L, 2)));
        when(orderMapper.updateById(pending)).thenReturn(1);

        assertEquals(1, service.cancelTimeoutOrders());

        verify(productSkuService).restoreStock(70L, 2, null, 11L);
        verify(orderMapper, times(3)).selectByIdForUpdate(any());
        verify(orderMapper).updateById(pending);
        assertEquals(OrderStatus.CANCELED, pending.getStatus());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listIsUserScopedOrderedAndLoadsAllItemsInOneBatch() {
        OrderListRequest request = new OrderListRequest();
        request.setPage(2);
        request.setSize(10);
        request.setStatus(OrderStatus.PAID);
        Order first = order(502L, 11L, OrderStatus.PAID);
        first.setOrderNo("NO502");
        first.setCreatedAt(LocalDateTime.of(2026, 7, 23, 10, 0));
        Order second = order(501L, 11L, OrderStatus.PAID);
        second.setOrderNo("NO501");
        Page<Order> selected = new Page<>(2, 10);
        selected.setTotal(2);
        selected.setRecords(List.of(first, second));
        when(orderMapper.selectPage(any(Page.class), any())).thenReturn(selected);
        OrderItem firstItem = item(1L, 502L, 70L, 2);
        firstItem.setProductTitle("商品7");
        firstItem.setPrice(new BigDecimal("12.00"));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(firstItem));

        PageResponse<OrderResponse> result = service.list(request);

        assertEquals(2L, result.getTotal());
        assertEquals(List.of("NO502", "NO501"),
                result.getList().stream().map(OrderResponse::getOrderNo).toList());
        assertEquals(1, result.getList().getFirst().getItems().size());
        assertEquals(0, result.getList().get(1).getItems().size());
        verify(orderItemMapper, times(1)).selectList(any());

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.Wrapper> wrapperCaptor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(orderMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertEquals(2L, pageCaptor.getValue().getCurrent());
        assertEquals(10L, pageCaptor.getValue().getSize());
        String sql = wrapperCaptor.getValue().getSqlSegment().toLowerCase();
        assertTrue(sql.contains("user_id"));
        assertTrue(sql.contains("status"));
        assertTrue(sql.contains("order by created_at desc,id desc"));
    }

    @Test
    void detailRejectsMissingAndForeignOrdersBeforeLoadingItems() {
        when(orderMapper.selectById(501L)).thenReturn(null);
        when(orderMapper.selectById(502L)).thenReturn(order(502L, 12L, OrderStatus.PAID));

        assertThrows(NoSuchElementException.class, () -> service.detail(501L));
        assertThrows(CustomForbiddenException.class, () -> service.detail(502L));

        verify(orderItemMapper, never()).selectList(any());
    }

    @Test
    void detailReturnsOnlyPersistedHeaderAddressAndItemSnapshots() {
        Order saved = order(501L, 11L, OrderStatus.PENDING_PAYMENT);
        saved.setOrderNo("NO501");
        saved.setReceiverName("张三");
        saved.setReceiverPhone("13800138000");
        saved.setReceiverProvince("广东省");
        saved.setReceiverCity("深圳市");
        saved.setReceiverDistrict("南山区");
        saved.setReceiverDetail("科技园1号");
        saved.setRemark("工作日送达");
        when(orderMapper.selectById(501L)).thenReturn(saved);
        OrderItem savedItem = item(1L, 501L, 70L, 2);
        savedItem.setProductTitle("下单时商品名");
        savedItem.setSkuTitle("下单时SKU");
        savedItem.setImage("snapshot.png");
        savedItem.setPrice(new BigDecimal("12.00"));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(savedItem));

        OrderDetailResponse result = service.detail(501L);

        assertEquals("NO501", result.getOrderNo());
        assertEquals("张三", result.getReceiverName());
        assertEquals("工作日送达", result.getRemark());
        assertEquals("下单时商品名", result.getItems().getFirst().getProductTitle());
        assertEquals(new BigDecimal("12.00"), result.getItems().getFirst().getPrice());
        verify(productMapper, never()).selectById(any());
        verify(productSkuMapper, never()).selectById(any());
    }

    private void stubCatalogAndWrites() {
        when(productSkuMapper.selectByIds(anyCollection())).thenReturn(List.of(sku(70L, 7L, "12.00")));
        when(productMapper.selectByIds(anyCollection()))
                .thenReturn(List.of(product(7L, "商品7", "7.png", 1)));
        when(identifierGenerator.nextId(any())).thenReturn(1001L);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);
        when(orderItemMapper.insert(any(OrderItem.class))).thenReturn(1);
    }

    private OrderCreateRequest cartRequest(List<Long> ids) {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setAddressId(1L);
        request.setSource("cart");
        request.setCartItemIds(ids);
        return request;
    }

    private OrderCreateRequest buyNowRequest() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setAddressId(1L);
        request.setSource("buyNow");
        request.setSkuId(70L);
        request.setQuantity(1);
        return request;
    }

    private OrderAddressSnapshot address(Long userId) {
        OrderAddressSnapshot address = new OrderAddressSnapshot();
        address.setId(1L);
        address.setUserId(userId);
        address.setRecipient("张三");
        address.setPhone("13800138000");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetail("科技园1号");
        return address;
    }

    private Cart cart(Long id, Long userId, Long skuId, int quantity, boolean checked) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setUserId(userId);
        cart.setProductSkuId(skuId);
        cart.setQuantity(quantity);
        cart.setChecked(checked);
        return cart;
    }

    private ProductSku sku(Long id, Long productId, String price) {
        ProductSku sku = new ProductSku();
        sku.setId(id);
        sku.setProductId(productId);
        sku.setTitle("SKU" + id);
        sku.setPrice(new BigDecimal(price));
        sku.setStock(10);
        return sku;
    }

    private Product product(Long id, String title, String image, int onSale) {
        Product product = new Product();
        product.setId(id);
        product.setTitle(title);
        product.setImage(image);
        product.setOnSale(onSale);
        return product;
    }

    private Order order(Long id, Long userId, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(status);
        return order;
    }

    private OrderItem item(Long id, Long orderId, Long skuId, int quantity) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrderId(orderId);
        item.setProductSkuId(skuId);
        item.setQuantity(quantity);
        return item;
    }
}
