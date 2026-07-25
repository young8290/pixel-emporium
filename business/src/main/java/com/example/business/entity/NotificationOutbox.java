package com.example.business.entity;
import com.example.business.enums.NotificationOutboxStatus;
import com.example.common.orm.entity.BaseEntity;
import lombok.Getter; import lombok.Setter; import java.time.LocalDateTime;
@Getter @Setter public class NotificationOutbox extends BaseEntity { private String eventId; private String eventType; private Long aggregateId; private NotificationOutboxStatus status; private Integer attempts; private LocalDateTime nextRetryAt; private LocalDateTime claimedAt; private LocalDateTime deliveredAt; private String lastError; }
