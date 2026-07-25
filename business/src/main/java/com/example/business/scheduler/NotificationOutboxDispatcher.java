package com.example.business.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.business.client.InternalMailClient;
import com.example.business.config.MailNotificationProperties;
import com.example.business.entity.NotificationOutbox;
import com.example.business.entity.Order;
import com.example.business.entity.OrderItem;
import com.example.business.enums.NotificationOutboxStatus;
import com.example.business.mapper.NotificationOutboxMapper;
import com.example.business.mapper.OrderItemMapper;
import com.example.business.mapper.OrderMapper;
import com.example.business.model.OrderCreatedMailEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class NotificationOutboxDispatcher {
    private static final int MAX_ATTEMPTS = 6;
    private static final long[] RETRY_BACKOFF_MINUTES = {1, 5, 30, 120, 360};

    private final NotificationOutboxMapper outbox;
    private final OrderMapper orders;
    private final OrderItemMapper items;
    private final InternalMailClient client;
    private final MailNotificationProperties properties;

    public NotificationOutboxDispatcher(NotificationOutboxMapper outbox, OrderMapper orders,
                                        OrderItemMapper items, InternalMailClient client, MailNotificationProperties properties) {
        this.outbox = outbox;
        this.orders = orders;
        this.items = items;
        this.client = client;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${notification.mail.dispatch-interval-ms:10000}")
    public void dispatch() {
        if (!client.validSecret()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        outbox.recoverStaleClaims(now, now.minusMinutes(10));
        List<NotificationOutbox> due = outbox.selectList(new LambdaQueryWrapper<NotificationOutbox>()
                .in(NotificationOutbox::getStatus, List.of(NotificationOutboxStatus.PENDING, NotificationOutboxStatus.RETRY))
                .and(query -> query.le(NotificationOutbox::getNextRetryAt, now)
                        .or()
                        .isNull(NotificationOutbox::getNextRetryAt))
                .last("limit " + Math.max(1, Math.min(100, properties.batchSize()))));
        for (NotificationOutbox row : due) {
            dispatch(row);
        }
    }

    private void dispatch(NotificationOutbox row) {
        LocalDateTime now = LocalDateTime.now();
        if (outbox.claim(row.getId(), now) != 1) {
            return;
        }
        int attempts = (row.getAttempts() == null ? 0 : row.getAttempts()) + 1;
        row.setAttempts(attempts);
        try {
            int status = client.post(event(row), row.getEventId());
            if (status != 200 && status != 202) {
                throw new IllegalStateException("internal mail endpoint returned HTTP " + status);
            }
            outbox.markSent(row.getId(), LocalDateTime.now());
        } catch (Exception exception) {
            row.setLastError(safe(exception));
            if (attempts >= MAX_ATTEMPTS) {
                row.setStatus(NotificationOutboxStatus.DEAD);
                row.setNextRetryAt(null);
            } else {
                row.setStatus(NotificationOutboxStatus.RETRY);
                row.setNextRetryAt(LocalDateTime.now().plusMinutes(
                        RETRY_BACKOFF_MINUTES[Math.min(attempts - 1, RETRY_BACKOFF_MINUTES.length - 1)]));
            }
            outbox.recordFailure(row.getId(), row.getStatus().name(), row.getNextRetryAt(), row.getLastError());
        }
    }

    private OrderCreatedMailEvent event(NotificationOutbox row) {
        Order order = orders.selectById(row.getAggregateId());
        List<OrderItem> lines = items.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, row.getAggregateId()));
        OrderCreatedMailEvent event = new OrderCreatedMailEvent();
        event.setEventId(row.getEventId());
        event.setEventType(row.getEventType());
        event.setOrderId(order.getId());
        event.setUserId(order.getUserId());
        event.setOrderNo(order.getOrderNo());
        event.setCreatedAt(order.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).toOffsetDateTime());
        event.setTotalAmount(order.getTotalAmount());
        event.setFreightAmount(order.getFreightAmount());
        event.setPayAmount(order.getPayAmount());
        event.setReceiver(receiver(order));
        event.setItems(lines.stream().map(this::item).toList());
        return event;
    }

    private OrderCreatedMailEvent.Receiver receiver(Order order) {
        OrderCreatedMailEvent.Receiver receiver = new OrderCreatedMailEvent.Receiver();
        receiver.setName(order.getReceiverName());
        receiver.setPhone(order.getReceiverPhone());
        receiver.setProvince(order.getReceiverProvince());
        receiver.setCity(order.getReceiverCity());
        receiver.setDistrict(order.getReceiverDistrict());
        receiver.setDetail(order.getReceiverDetail());
        return receiver;
    }

    private OrderCreatedMailEvent.Item item(OrderItem line) {
        OrderCreatedMailEvent.Item item = new OrderCreatedMailEvent.Item();
        item.setProductId(line.getProductId());
        item.setProductTitle(line.getProductTitle());
        item.setSkuTitle(line.getSkuTitle());
        item.setImage(line.getImage());
        item.setPrice(line.getPrice());
        item.setQuantity(line.getQuantity());
        item.setSubtotal(line.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        return item;
    }

    private String safe(Exception exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().replaceAll("[\\r\\n]", " ");
        return exception.getClass().getSimpleName() + ": " + message.substring(0, Math.min(400, message.length()));
    }
}
