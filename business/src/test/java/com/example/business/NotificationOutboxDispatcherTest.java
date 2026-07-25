package com.example.business;

import com.example.business.client.InternalMailClient;
import com.example.business.config.MailNotificationProperties;
import com.example.business.entity.NotificationOutbox;
import com.example.business.entity.Order;
import com.example.business.enums.NotificationOutboxStatus;
import com.example.business.mapper.NotificationOutboxMapper;
import com.example.business.mapper.OrderItemMapper;
import com.example.business.mapper.OrderMapper;
import com.example.business.scheduler.NotificationOutboxDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationOutboxDispatcherTest {
    private NotificationOutboxMapper outbox;
    private OrderMapper orders;
    private OrderItemMapper items;
    private InternalMailClient client;

    @BeforeEach
    void setUp() {
        outbox = mock(NotificationOutboxMapper.class);
        orders = mock(OrderMapper.class);
        items = mock(OrderItemMapper.class);
        client = mock(InternalMailClient.class);
    }

    @Test
    void missingSecretLeavesPendingEventsUnclaimed() throws Exception {
        when(client.validSecret()).thenReturn(false);

        dispatcher().dispatch();

        verifyNoInteractions(outbox);
        verify(client, never()).post(any(), anyString());
    }

    @Test
    void acceptedInternalRequestMarksClaimedEventSent() throws Exception {
        NotificationOutbox row = dueRow(0);
        stubDue(row);
        when(client.post(any(), anyString())).thenReturn(202);

        dispatcher().dispatch();

        verify(outbox).recoverStaleClaims(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(outbox).claim(eq(11L), any(LocalDateTime.class));
        verify(outbox).markSent(eq(11L), any(LocalDateTime.class));
        verify(outbox, never()).recordFailure(any(), anyString(), any(), anyString());
    }

    @Test
    void transportFailureIsPersistedForRetryAfterClaim() throws Exception {
        NotificationOutbox row = dueRow(0);
        stubDue(row);
        when(client.post(any(), anyString())).thenThrow(new IllegalStateException("connection refused"));

        dispatcher().dispatch();

        assertEquals(NotificationOutboxStatus.RETRY, row.getStatus());
        assertEquals(1, row.getAttempts());
        assertTrue(row.getNextRetryAt().isAfter(LocalDateTime.now()));
        verify(outbox).recordFailure(eq(11L), eq("RETRY"), any(LocalDateTime.class), anyString());
    }

    @Test
    void sixthFailureMovesEventToDeadWithoutAnotherRetry() throws Exception {
        NotificationOutbox row = dueRow(5);
        stubDue(row);
        when(client.post(any(), anyString())).thenThrow(new IllegalStateException("connection refused"));

        dispatcher().dispatch();

        assertEquals(NotificationOutboxStatus.DEAD, row.getStatus());
        assertEquals(6, row.getAttempts());
        assertNull(row.getNextRetryAt());
        verify(outbox).recordFailure(eq(11L), eq("DEAD"), eq(null), anyString());
    }

    private void stubDue(NotificationOutbox row) {
        when(client.validSecret()).thenReturn(true);
        when(outbox.selectList(any())).thenReturn(List.of(row));
        when(outbox.claim(eq(11L), any(LocalDateTime.class))).thenReturn(1);
        when(orders.selectById(99L)).thenReturn(order());
        when(items.selectList(any())).thenReturn(List.of());
    }

    private NotificationOutbox dueRow(int attempts) {
        NotificationOutbox row = new NotificationOutbox();
        row.setId(11L);
        row.setEventId("ORDER_CREATED:99");
        row.setEventType("ORDER_CREATED");
        row.setAggregateId(99L);
        row.setStatus(NotificationOutboxStatus.PENDING);
        row.setAttempts(attempts);
        return row;
    }

    private Order order() {
        Order order = new Order();
        order.setId(99L);
        order.setUserId(42L);
        order.setOrderNo("ORDER-99");
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalAmount(new BigDecimal("20.00"));
        order.setFreightAmount(BigDecimal.ZERO);
        order.setPayAmount(new BigDecimal("20.00"));
        order.setReceiverName("Buyer");
        return order;
    }

    private NotificationOutboxDispatcher dispatcher() {
        return new NotificationOutboxDispatcher(outbox, orders, items, client,
                new MailNotificationProperties("http://localhost", secret(), 100, 100, 10_000, 20));
    }

    private String secret() {
        return "01234567890123456789012345678901";
    }
}
