package com.example.business;

import com.example.business.mapper.NotificationOutboxMapper;
import com.example.business.service.NotificationOutboxService;
import com.example.business.entity.NotificationOutbox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderNotificationOutboxTest {
    @Test
    void outboxWriteUsesOrderEventIdAndFailsTheTransactionWhenInsertFails() {
        NotificationOutboxMapper mapper = mock(NotificationOutboxMapper.class);
        when(mapper.insert(any(NotificationOutbox.class))).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> new NotificationOutboxService(mapper).appendOrderCreated(99L));
        verify(mapper).insert(argThat((NotificationOutbox row) -> "ORDER_CREATED:99".equals(row.getEventId())
                && "ORDER_CREATED".equals(row.getEventType()) && Long.valueOf(99L).equals(row.getAggregateId())));
    }
}
