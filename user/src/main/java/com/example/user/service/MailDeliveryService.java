package com.example.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.exception.CustomBusinessException;
import com.example.common.exception.CustomConflictException;
import com.example.common.response.PageResponse;
import com.example.user.entity.MailConfig;
import com.example.user.entity.MailDelivery;
import com.example.user.entity.User;
import com.example.user.enums.MailDeliveryStatus;
import com.example.user.enums.MailTemplateKey;
import com.example.user.mapper.MailDeliveryMapper;
import com.example.user.model.OrderCreatedMailEvent;
import com.example.user.request.MailDeliveryListRequest;
import com.example.user.response.MailDeliveryResponse;
import com.example.user.security.MailSecretCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MailDeliveryService {
    private static final long[] RETRY_BACKOFF_MINUTES = {1, 5, 30, 120, 360};

    private final MailDeliveryMapper mapper;
    private final MailSecretCipher cipher;
    private final ObjectMapper json;
    private final MailTemplateService templates;
    private final MailConfigService config;
    private final UserService users;

    public MailDeliveryService(MailDeliveryMapper mapper, MailSecretCipher cipher, ObjectMapper json,
                               MailTemplateService templates, MailConfigService config, UserService users) {
        this.mapper = mapper;
        this.cipher = cipher;
        this.json = json;
        this.templates = templates;
        this.config = config;
        this.users = users;
    }

    public boolean accept(OrderCreatedMailEvent event) {
        if (mapper.selectOne(new LambdaQueryWrapper<MailDelivery>()
                .eq(MailDelivery::getEventId, event.getEventId())) != null) {
            return false;
        }

        MailDelivery delivery = new MailDelivery();
        delivery.setId(IdWorker.getId());
        delivery.setEventId(event.getEventId());
        delivery.setTemplateKey(MailTemplateKey.ORDER_CREATED);
        delivery.setSubject("【像素百货】订单 " + event.getOrderNo() + " 已创建");
        delivery.setAttempts(0);
        delivery.setMaxAttempts(6);
        try {
            delivery.setModelCiphertext(cipher.encryptDeliveryModel(json.writeValueAsString(event)));
        } catch (Exception e) {
            throw new CustomBusinessException("邮件事件保存失败", 503);
        }

        User user = users.getById(event.getUserId());
        if (!hasUsableEmail(user)) {
            delivery.setRecipient(null);
            delivery.setStatus(MailDeliveryStatus.DEAD);
            delivery.setLastError("RecipientUnavailable");
        } else {
            delivery.setRecipient(user.getEmail());
            delivery.setStatus(MailDeliveryStatus.PENDING);
        }
        try {
            mapper.insert(delivery);
        } catch (DuplicateKeyException ignored) {
            return false;
        }
        return true;
    }

    public void deliverDue() {
        LocalDateTime now = LocalDateTime.now();
        mapper.recoverStaleClaims(now, now.minusMinutes(10));
        List<MailDelivery> deliveries = mapper.selectList(new LambdaQueryWrapper<MailDelivery>()
                .in(MailDelivery::getStatus, List.of(MailDeliveryStatus.PENDING, MailDeliveryStatus.RETRY))
                .and(query -> query.le(MailDelivery::getNextRetryAt, now)
                        .or()
                        .isNull(MailDelivery::getNextRetryAt))
                .last("limit 20"));
        for (MailDelivery delivery : deliveries) {
            if (mapper.claim(delivery.getId(), now) == 1) {
                delivery.setStatus(MailDeliveryStatus.SENDING);
                delivery.setClaimedAt(now);
                delivery.setAttempts((delivery.getAttempts() == null ? 0 : delivery.getAttempts()) + 1);
                send(delivery);
            }
        }
    }

    public void send(MailDelivery delivery) {
        try {
            OrderCreatedMailEvent event = json.readValue(cipher.decryptDeliveryModel(delivery.getModelCiphertext()),
                    OrderCreatedMailEvent.class);
            MailTemplateService.RenderedMail mail = delivery.getTemplateKey() == MailTemplateKey.VERIFICATION_CODE
                    ? templates.verification("123456") : templates.orderCreated(event);
            var sender = config.sender();
            var message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            MailConfig smtp = config.current();
            helper.setTo(delivery.getRecipient());
            helper.setFrom(smtp.getFromAddress(), smtp.getFromName());
            if (smtp.getReplyTo() != null && !smtp.getReplyTo().isBlank()) {
                helper.setReplyTo(smtp.getReplyTo());
            }
            helper.setSubject(mail.subject());
            helper.setText(mail.text(), mail.html());
            sender.send(message);
        } catch (Exception exception) {
            recordFailure(delivery, exception);
            return;
        }

        LocalDateTime sentAt = LocalDateTime.now();
        if (mapper.markSent(delivery.getId(), sentAt) != 1) {
            throw new CustomBusinessException("邮件投递状态更新失败", 503);
        }
        delivery.setStatus(MailDeliveryStatus.SENT);
        delivery.setSentAt(sentAt);
        delivery.setClaimedAt(null);
        delivery.setNextRetryAt(null);
        delivery.setLastError(null);
    }

    public void queueTest(String recipient, MailTemplateKey key) {
        MailDelivery delivery = new MailDelivery();
        delivery.setId(IdWorker.getId());
        delivery.setEventId("TEST:" + UUID.randomUUID());
        delivery.setTemplateKey(key);
        delivery.setRecipient(recipient);
        delivery.setSubject("【像素百货】测试邮件");
        delivery.setStatus(MailDeliveryStatus.PENDING);
        delivery.setAttempts(0);
        delivery.setMaxAttempts(6);
        try {
            OrderCreatedMailEvent event = new OrderCreatedMailEvent();
            event.setOrderId(1L);
            event.setOrderNo("TEST-ORDER");
            event.setTotalAmount(java.math.BigDecimal.ZERO);
            event.setFreightAmount(java.math.BigDecimal.ZERO);
            event.setPayAmount(java.math.BigDecimal.ZERO);
            event.setCreatedAt(java.time.OffsetDateTime.now());
            OrderCreatedMailEvent.Receiver receiver = new OrderCreatedMailEvent.Receiver();
            receiver.setName("测试用户");
            receiver.setPhone("13800000000");
            receiver.setProvince("广东省");
            receiver.setCity("深圳市");
            receiver.setDistrict("南山区");
            receiver.setDetail("示例路 1 号");
            event.setReceiver(receiver);
            event.setItems(List.of());
            delivery.setModelCiphertext(cipher.encryptDeliveryModel(json.writeValueAsString(event)));
        } catch (Exception e) {
            throw new CustomBusinessException("测试邮件创建失败");
        }
        mapper.insert(delivery);
    }

    public PageResponse<MailDeliveryResponse> list(MailDeliveryListRequest request) {
        Page<MailDelivery> page = mapper.selectPage(new Page<>(request.getPage(), request.getSize()),
                new LambdaQueryWrapper<MailDelivery>()
                        .eq(request.getStatus() != null, MailDelivery::getStatus, request.getStatus())
                        .eq(request.getTemplateKey() != null, MailDelivery::getTemplateKey, request.getTemplateKey())
                        .like(request.getRecipient() != null && !request.getRecipient().isBlank(),
                                MailDelivery::getRecipient, request.getRecipient())
                        .like(request.getEventId() != null && !request.getEventId().isBlank(),
                                MailDelivery::getEventId, request.getEventId())
                        .orderByDesc(MailDelivery::getCreatedAt));
        PageResponse<MailDeliveryResponse> response = new PageResponse<>();
        response.setTotal(page.getTotal());
        response.setList(page.getRecords().stream().map(this::response).toList());
        return response;
    }

    public void retry(Long id) {
        MailDelivery delivery = mapper.selectById(id);
        if (delivery == null) {
            throw new NoSuchElementException("投递记录不存在");
        }
        if (delivery.getStatus() != MailDeliveryStatus.DEAD) {
            throw new CustomConflictException("只有失败邮件可以重试");
        }
        restoreRecipientIfNeeded(delivery);
        if (mapper.resetForManualRetry(delivery.getId(), delivery.getRecipient()) != 1) {
            throw new CustomBusinessException("邮件投递状态更新失败", 503);
        }
        delivery.setStatus(MailDeliveryStatus.PENDING);
        delivery.setAttempts(0);
        delivery.setClaimedAt(null);
        delivery.setNextRetryAt(null);
        delivery.setLastError(null);
    }

    private void recordFailure(MailDelivery delivery, Exception exception) {
        int attempts = delivery.getAttempts() == null ? 1 : delivery.getAttempts();
        String lastError = exception.getClass().getSimpleName() + ": " + safe(exception.getMessage());
        int maxAttempts = delivery.getMaxAttempts() != null && delivery.getMaxAttempts() > 0
                ? delivery.getMaxAttempts() : 6;
        MailDeliveryStatus status = attempts >= maxAttempts
                ? MailDeliveryStatus.DEAD : MailDeliveryStatus.RETRY;
        LocalDateTime nextRetryAt = status == MailDeliveryStatus.DEAD ? null
                : LocalDateTime.now().plusMinutes(
                        RETRY_BACKOFF_MINUTES[Math.min(attempts - 1, RETRY_BACKOFF_MINUTES.length - 1)]);
        if (mapper.recordFailure(delivery.getId(), status.name(), attempts, nextRetryAt, lastError) != 1) {
            throw new CustomBusinessException("邮件投递状态更新失败", 503);
        }
        delivery.setAttempts(attempts);
        delivery.setStatus(status);
        delivery.setClaimedAt(null);
        delivery.setNextRetryAt(nextRetryAt);
        delivery.setLastError(lastError);
    }

    private void restoreRecipientIfNeeded(MailDelivery delivery) {
        if (!"RecipientUnavailable".equals(delivery.getLastError())) {
            return;
        }
        try {
            OrderCreatedMailEvent event = json.readValue(cipher.decryptDeliveryModel(delivery.getModelCiphertext()),
                    OrderCreatedMailEvent.class);
            User user = users.getById(event.getUserId());
            if (!hasUsableEmail(user)) {
                throw new CustomConflictException("用户当前没有可用邮箱");
            }
            delivery.setRecipient(user.getEmail());
        } catch (CustomConflictException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomConflictException("无法恢复邮件收件人");
        }
    }

    private boolean hasUsableEmail(User user) {
        return user != null && !Boolean.FALSE.equals(user.getEnabled())
                && user.getEmail() != null && !user.getEmail().isBlank();
    }

    private MailDeliveryResponse response(MailDelivery delivery) {
        MailDeliveryResponse response = new MailDeliveryResponse();
        response.setId(delivery.getId());
        response.setEventId(delivery.getEventId());
        response.setTemplateKey(delivery.getTemplateKey());
        response.setRecipient(delivery.getRecipient());
        response.setSubject(delivery.getSubject());
        response.setStatus(delivery.getStatus());
        response.setAttempts(delivery.getAttempts());
        response.setSentAt(delivery.getSentAt());
        response.setLastError(delivery.getLastError());
        response.setCreatedAt(delivery.getCreatedAt());
        return response;
    }

    private String safe(String message) {
        if (message == null) {
            return "";
        }
        String sanitized = message.replaceAll("[\\r\\n]", " ");
        return sanitized.substring(0, Math.min(400, sanitized.length()));
    }
}
