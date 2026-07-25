package com.example.business;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.business.entity.Order;
import com.example.business.entity.OrderItem;
import com.example.business.enums.OrderStatus;
import com.example.business.mapper.CartMapper;
import com.example.business.mapper.OrderItemMapper;
import com.example.business.mapper.OrderMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.request.OrderAdminListRequest;
import com.example.business.response.OrderAdminDetailResponse;
import com.example.business.response.OrderAdminResponse;
import com.example.business.service.ProductSkuService;
import com.example.business.service.impl.OrderServiceImpl;
import com.example.common.exception.CustomValidationException;
import com.example.common.response.PageResponse;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAdminServiceTest {
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private CartMapper cartMapper;
    @Mock
    private ProductSkuMapper productSkuMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductSkuService productSkuService;
    @Mock
    private IdentifierGenerator identifierGenerator;

    private OrderServiceImpl service;

    @BeforeAll
    static void initializeMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "order-admin-test");
        TableInfoHelper.initTableInfo(assistant, Order.class);
        TableInfoHelper.initTableInfo(assistant, OrderItem.class);
    }

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderMapper, orderItemMapper, cartMapper,
                productSkuMapper, productMapper, productSkuService, identifierGenerator);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void adminListAppliesAllFiltersStableOrderAndDoesNotLoadItems() {
        OrderAdminListRequest request = new OrderAdminListRequest();
        request.setOrderNo(" NO50 ");
        request.setUserId(11L);
        request.setStatus(OrderStatus.PAID);
        request.setCreatedAtStart(LocalDateTime.of(2026, 7, 1, 0, 0));
        request.setCreatedAtEnd(LocalDateTime.of(2026, 7, 31, 23, 59));
        request.setPage(2);
        request.setSize(10);
        Order order = order(501L, 11L, OrderStatus.PAID);
        order.setOrderNo("NO501");
        Page<Order> page = new Page<>(2, 10);
        page.setTotal(1);
        page.setRecords(List.of(order));
        when(orderMapper.selectPage(any(Page.class), any())).thenReturn(page);

        PageResponse<OrderAdminResponse> result = service.adminList(request);

        assertEquals(1L, result.getTotal());
        assertEquals(11L, result.getList().getFirst().getUserId());
        verifyNoInteractions(orderItemMapper);
        ArgumentCaptor<Wrapper> wrapper = ArgumentCaptor.forClass(Wrapper.class);
        verify(orderMapper).selectPage(any(Page.class), wrapper.capture());
        String sql = wrapper.getValue().getSqlSegment().toLowerCase();
        assertTrue(sql.contains("order_no") && sql.contains("user_id"));
        assertTrue(sql.contains("status") && sql.contains("created_at"));
        assertTrue(sql.contains("order by created_at desc,id desc"));
    }

    @Test
    void adminListRejectsReversedTimeRangeBeforeQuery() {
        OrderAdminListRequest request = new OrderAdminListRequest();
        request.setCreatedAtStart(LocalDateTime.of(2026, 8, 1, 0, 0));
        request.setCreatedAtEnd(LocalDateTime.of(2026, 7, 1, 0, 0));

        assertThrows(CustomValidationException.class, () -> service.adminList(request));
        verifyNoInteractions(orderMapper);
    }

    @Test
    void adminListRejectsNullPageOrSizeBeforeQuery() {
        OrderAdminListRequest nullPage = new OrderAdminListRequest();
        nullPage.setPage(null);
        assertThrows(CustomValidationException.class, () -> service.adminList(nullPage));

        OrderAdminListRequest nullSize = new OrderAdminListRequest();
        nullSize.setSize(null);
        assertThrows(CustomValidationException.class, () -> service.adminList(nullSize));

        verifyNoInteractions(orderMapper);
    }

    @Test
    void adminDetailReturnsSnapshotsAndItemsWithoutOwnershipCheck() {
        Order order = order(501L, 99L, OrderStatus.PAID);
        order.setReceiverName("张三");
        order.setReceiverDetail("科技园1号");
        when(orderMapper.selectById(501L)).thenReturn(order);
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrderId(501L);
        item.setProductTitle("下单快照商品");
        item.setQuantity(2);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        OrderAdminDetailResponse result = service.adminDetail(501L);

        assertEquals(99L, result.getUserId());
        assertEquals("张三", result.getReceiverName());
        assertEquals("下单快照商品", result.getItems().getFirst().getProductTitle());
    }

    @Test
    void adminDetailRejectsMissingOrderBeforeLoadingItems() {
        when(orderMapper.selectById(501L)).thenReturn(null);

        assertThrows(java.util.NoSuchElementException.class, () -> service.adminDetail(501L));
        verify(orderItemMapper, never()).selectList(any());
    }

    private Order order(Long id, Long userId, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(status);
        return order;
    }
}
