package com.example.business;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.example.business.entity.Order;
import com.example.business.enums.OrderStatus;
import com.example.business.mapper.CartMapper;
import com.example.business.mapper.OrderItemMapper;
import com.example.business.mapper.OrderMapper;
import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductSkuMapper;
import com.example.business.service.ProductSkuService;
import com.example.business.request.OrderShipRequest;
import com.example.business.service.impl.OrderServiceImpl;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderShipTest {
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

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderMapper, orderItemMapper, cartMapper,
                productSkuMapper, productMapper, productSkuService, identifierGenerator);
    }

    @Test
    void paidOrderShipsAndRecordsTime() {
        Order paid = order(501L, OrderStatus.PAID);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(paid);
        when(orderMapper.updateById(paid)).thenReturn(1);
        LocalDateTime before = LocalDateTime.now();

        assertTrue(service.ship(501L, shipRequest()));

        assertEquals(OrderStatus.SHIPPED, paid.getStatus());
        assertNotNull(paid.getShippedAt());
        assertEquals("像素快递", paid.getShippingCarrier());
        assertEquals("PX20260725001", paid.getTrackingNumber());
        assertEquals("请轻放", paid.getShippingNote());
        assertTrue(!paid.getShippedAt().isBefore(before));
        InOrder inOrder = inOrder(orderMapper);
        inOrder.verify(orderMapper).selectByIdForUpdate(501L);
        inOrder.verify(orderMapper).updateById(paid);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class,
            names = {"PENDING_PAYMENT", "SHIPPED", "COMPLETED", "CANCELED"})
    void everyNonPaidStateIsRejected(OrderStatus status) {
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(order(501L, status));

        assertThrows(CustomConflictException.class, () -> service.ship(501L, shipRequest()));
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    void repeatedShipObservesLockedShippedStateAndIsRejected() {
        Order paid = order(501L, OrderStatus.PAID);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(paid);
        when(orderMapper.updateById(paid)).thenReturn(1);

        assertTrue(service.ship(501L, shipRequest()));
        assertThrows(CustomConflictException.class, () -> service.ship(501L, shipRequest()));

        verify(orderMapper, times(2)).selectByIdForUpdate(501L);
        verify(orderMapper, times(1)).updateById(paid);
    }

    @Test
    void updateCountMismatchFailsTheTransaction() {
        Order paid = order(501L, OrderStatus.PAID);
        when(orderMapper.selectByIdForUpdate(501L)).thenReturn(paid);
        when(orderMapper.updateById(paid)).thenReturn(0);

        assertThrows(CustomBusinessException.class, () -> service.ship(501L, shipRequest()));
    }

    private Order order(Long id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        return order;
    }

    private OrderShipRequest shipRequest() {
        OrderShipRequest request = new OrderShipRequest();
        request.setShippingCarrier(" 像素快递 ");
        request.setTrackingNumber(" PX20260725001 ");
        request.setShippingNote(" 请轻放 ");
        return request;
    }
}
