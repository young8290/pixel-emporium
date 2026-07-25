package com.example.user.entity;

import com.example.common.orm.entity.BaseEntity;
import com.example.user.enums.MailDeliveryStatus;
import com.example.user.enums.MailTemplateKey;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class MailDelivery extends BaseEntity {
    private String eventId; private MailTemplateKey templateKey; private String recipient; private String subject;
    private String modelCiphertext; private MailDeliveryStatus status; private Integer attempts; private Integer maxAttempts;
    private LocalDateTime nextRetryAt; private LocalDateTime claimedAt; private LocalDateTime sentAt; private String lastError;
}
