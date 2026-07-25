package com.example.user;

import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomBusinessException;
import com.example.user.entity.MailConfig;
import com.example.user.entity.MailDelivery;
import com.example.user.entity.User;
import com.example.user.enums.MailDeliveryStatus;
import com.example.user.mapper.MailDeliveryMapper;
import com.example.user.model.OrderCreatedMailEvent;
import com.example.user.security.MailSecretCipher;
import com.example.user.service.MailConfigService;
import com.example.user.service.MailDeliveryService;
import com.example.user.service.MailTemplateService;
import com.example.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.Invocation;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MailDeliveryServiceTest {
    private MailDeliveryMapper mapper;
    private MailSecretCipher cipher;
    private UserService users;
    private MailTemplateService templates;
    private MailConfigService config;
    private MailDeliveryService service;
    private ObjectMapper json;
    private int transitionUpdateRows;

    @BeforeEach
    void setUp() {
        transitionUpdateRows = 1;
        mapper = mock(MailDeliveryMapper.class, invocation -> {
            String methodName = invocation.getMethod().getName();
            if (invocation.getMethod().getReturnType() == int.class
                    && (methodName.equals("markSent")
                    || methodName.equals("recordFailure")
                    || methodName.equals("resetForManualRetry"))) {
                return transitionUpdateRows;
            }
            return RETURNS_DEFAULTS.answer(invocation);
        });
        cipher = mock(MailSecretCipher.class);
        users = mock(UserService.class);
        templates = mock(MailTemplateService.class);
        config = mock(MailConfigService.class);
        json = new ObjectMapper().findAndRegisterModules();
        service = new MailDeliveryService(mapper, cipher, json, templates, config, users);
    }

    @Test
    void existingEventIsIdempotentWithoutEncryptingOrLookingUpRecipient() {
        MailDelivery existing = new MailDelivery();
        when(mapper.selectOne(any())).thenReturn(existing);

        assertFalse(service.accept(event(42L)));

        verifyNoInteractions(cipher, users);
        verify(mapper, never()).insert(any(MailDelivery.class));
    }

    @Test
    void duplicateKeyRaceIsAlsoTreatedAsIdempotentSuccess() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(cipher.encryptDeliveryModel(anyString())).thenReturn("ciphertext");
        when(users.getById(42L)).thenReturn(enabledUser("owner@example.com"));
        when(mapper.insert(any(MailDelivery.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertFalse(service.accept(event(42L)));
    }

    @Test
    void unavailableRecipientCreatesDeadAuditableDelivery() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(cipher.encryptDeliveryModel(anyString())).thenReturn("ciphertext");
        when(users.getById(42L)).thenReturn(null);
        when(mapper.insert(any(MailDelivery.class))).thenReturn(1);

        assertTrue(service.accept(event(42L)));

        ArgumentCaptor<MailDelivery> saved = ArgumentCaptor.forClass(MailDelivery.class);
        verify(mapper).insert(saved.capture());
        assertEquals(MailDeliveryStatus.DEAD, saved.getValue().getStatus());
        assertEquals("RecipientUnavailable", saved.getValue().getLastError());
        assertNull(saved.getValue().getRecipient());
    }

    @Test
    void retryKeepsRecipientUnavailableDeliveryDeadUntilAccountHasUsableEmail() throws Exception {
        MailDelivery delivery = new MailDelivery();
        delivery.setId(7L);
        delivery.setStatus(MailDeliveryStatus.DEAD);
        delivery.setLastError("RecipientUnavailable");
        delivery.setModelCiphertext("ciphertext");
        when(mapper.selectById(7L)).thenReturn(delivery);

        OrderCreatedMailEvent event = new OrderCreatedMailEvent();
        event.setUserId(42L);
        when(cipher.decryptDeliveryModel("ciphertext")).thenReturn(new ObjectMapper().writeValueAsString(event));
        User disabled = new User();
        disabled.setEnabled(false);
        disabled.setEmail("owner@example.com");
        when(users.getById(42L)).thenReturn(disabled);

        assertThrows(CustomConflictException.class, () -> service.retry(7L));

        verify(mapper, never()).updateById(any(MailDelivery.class));
    }

    @Test
    void retryRestoresRecipientFromEncryptedEventAfterAccountRecovery() throws Exception {
        MailDelivery delivery = new MailDelivery();
        delivery.setId(7L);
        delivery.setStatus(MailDeliveryStatus.DEAD);
        delivery.setLastError("RecipientUnavailable");
        delivery.setModelCiphertext("ciphertext");
        when(mapper.selectById(7L)).thenReturn(delivery);
        when(cipher.decryptDeliveryModel("ciphertext"))
                .thenReturn(json.writeValueAsString(event(42L)));
        when(users.getById(42L)).thenReturn(enabledUser("owner@example.com"));

        service.retry(7L);

        assertEquals(MailDeliveryStatus.PENDING, delivery.getStatus());
        assertEquals("owner@example.com", delivery.getRecipient());
        assertEquals(0, delivery.getAttempts());
        assertNull(delivery.getLastError());
        Invocation reset = invocationNamed("resetForManualRetry");
        assertNotNull(reset);
        assertEquals(7L, reset.getArguments()[0]);
        assertEquals("owner@example.com", reset.getArguments()[1]);
        assertFalse(wasInvoked("updateById"));
    }

    @Test
    void deliverDueRecoversStaleClaimsAndClaimsEachDeliveryBeforeSending() {
        MailDelivery delivery = new MailDelivery();
        delivery.setId(8L);
        delivery.setStatus(MailDeliveryStatus.PENDING);
        delivery.setAttempts(0);
        when(mapper.selectList(any())).thenReturn(java.util.List.of(delivery));
        when(mapper.claim(eq(8L), any(LocalDateTime.class))).thenReturn(1);

        service.deliverDue();

        verify(mapper).recoverStaleClaims(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(mapper).claim(eq(8L), any(LocalDateTime.class));
        org.junit.jupiter.api.Assertions.assertEquals(1, delivery.getAttempts());
    }

    @Test
    void orderCreatedTestDeliveryHasRenderableModelAndUsesReplyTo() throws Exception {
        JavaMailSenderImpl sender = mock(JavaMailSenderImpl.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        service = new MailDeliveryService(mapper, cipher, json, templates, config, users);

        when(cipher.encryptDeliveryModel(any())).thenReturn("test-ciphertext");
        service.queueTest("buyer@example.com", com.example.user.enums.MailTemplateKey.ORDER_CREATED);

        ArgumentCaptor<MailDelivery> queued = ArgumentCaptor.forClass(MailDelivery.class);
        ArgumentCaptor<String> encryptedModel = ArgumentCaptor.forClass(String.class);
        verify(mapper).insert(queued.capture());
        verify(cipher).encryptDeliveryModel(encryptedModel.capture());
        MailDelivery delivery = queued.getValue();
        assertEquals(com.example.user.enums.MailTemplateKey.ORDER_CREATED, delivery.getTemplateKey());
        assertEquals("test-ciphertext", delivery.getModelCiphertext());
        String eventJson = encryptedModel.getValue();
        OrderCreatedMailEvent queuedEvent = json.readValue(eventJson, OrderCreatedMailEvent.class);
        assertEquals("TEST-ORDER", queuedEvent.getOrderNo());
        assertEquals(1L, queuedEvent.getOrderId());
        assertEquals("测试用户", queuedEvent.getReceiver().getName());
        assertEquals(0, queuedEvent.getItems().size());

        when(cipher.decryptDeliveryModel("test-ciphertext")).thenReturn(eventJson);
        when(templates.orderCreated(any())).thenReturn(new MailTemplateService.RenderedMail("subject", "<p>html</p>", "text"));
        MailConfig smtp = new MailConfig();
        smtp.setFromAddress("shop@example.com");
        smtp.setFromName("Pixel Shop");
        smtp.setReplyTo("support@example.com");
        when(config.sender()).thenReturn(sender);
        when(config.current()).thenReturn(smtp);
        when(sender.createMimeMessage()).thenReturn(message);

        delivery.setAttempts(1);
        service.send(delivery);

        assertEquals(MailDeliveryStatus.SENT, delivery.getStatus(), delivery.getLastError());
        assertEquals("support@example.com", message.getReplyTo()[0].toString());
        verify(sender).send(message);
        assertNotNull(invocationNamed("markSent"));
        assertFalse(wasInvoked("updateById"));
    }

    @Test
    void transientSendFailureSchedulesRetryAndSanitizesStoredError() throws Exception {
        MailDelivery delivery = deliveryForSend(1);
        when(config.sender()).thenThrow(new CustomBusinessException("smtp\r\ndown", 503));

        service.send(delivery);

        assertEquals(MailDeliveryStatus.RETRY, delivery.getStatus());
        assertNotNull(delivery.getNextRetryAt());
        assertFalse(delivery.getLastError().contains("\r"));
        assertFalse(delivery.getLastError().contains("\n"));
        Invocation failure = invocationNamed("recordFailure");
        assertNotNull(failure);
        assertEquals(9L, failure.getArguments()[0]);
        assertEquals("RETRY", failure.getArguments()[1]);
        assertEquals(1, failure.getArguments()[2]);
        assertNotNull(failure.getArguments()[3]);
        assertEquals(delivery.getLastError(), failure.getArguments()[4]);
        assertFalse(wasInvoked("updateById"));
    }

    @Test
    void sixthSendFailureMovesDeliveryToDead() throws Exception {
        MailDelivery delivery = deliveryForSend(6);
        when(config.sender()).thenThrow(new CustomBusinessException("smtp down", 503));

        service.send(delivery);

        assertEquals(MailDeliveryStatus.DEAD, delivery.getStatus());
        assertEquals(6, delivery.getAttempts());
        Invocation failure = invocationNamed("recordFailure");
        assertNotNull(failure);
        assertEquals("DEAD", failure.getArguments()[1]);
        assertEquals(6, failure.getArguments()[2]);
        assertNull(failure.getArguments()[3]);
        assertEquals(delivery.getLastError(), failure.getArguments()[4]);
        assertFalse(wasInvoked("updateById"));
    }

    @Test
    void retryFailsWithBusinessErrorWhenStateTransitionUpdatesNoRows() {
        MailDelivery delivery = new MailDelivery();
        delivery.setId(7L);
        delivery.setStatus(MailDeliveryStatus.DEAD);
        delivery.setRecipient("owner@example.com");
        delivery.setLastError("smtp down");
        when(mapper.selectById(7L)).thenReturn(delivery);
        transitionUpdateRows = 0;

        CustomBusinessException exception = assertThrows(CustomBusinessException.class,
                () -> service.retry(7L));

        assertEquals("邮件投递状态更新失败", exception.getMessage());
        assertFalse(wasInvoked("updateById"));
    }

    private Invocation invocationNamed(String methodName) {
        return mockingDetails(mapper).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals(methodName))
                .findFirst()
                .orElse(null);
    }

    private boolean wasInvoked(String methodName) {
        return invocationNamed(methodName) != null;
    }

    private MailDelivery deliveryForSend(int attempts) throws Exception {
        OrderCreatedMailEvent event = event(42L);
        String eventJson = json.writeValueAsString(event);
        when(cipher.decryptDeliveryModel("ciphertext")).thenReturn(eventJson);
        when(templates.orderCreated(any())).thenReturn(
                new MailTemplateService.RenderedMail("subject", "<p>html</p>", "text"));
        MailDelivery delivery = new MailDelivery();
        delivery.setId(9L);
        delivery.setRecipient("owner@example.com");
        delivery.setTemplateKey(com.example.user.enums.MailTemplateKey.ORDER_CREATED);
        delivery.setModelCiphertext("ciphertext");
        delivery.setStatus(MailDeliveryStatus.SENDING);
        delivery.setAttempts(attempts);
        delivery.setMaxAttempts(6);
        return delivery;
    }

    private OrderCreatedMailEvent event(Long userId) {
        OrderCreatedMailEvent event = new OrderCreatedMailEvent();
        event.setEventId("ORDER_CREATED:99");
        event.setEventType("ORDER_CREATED");
        event.setOrderId(99L);
        event.setUserId(userId);
        event.setOrderNo("ORDER-99");
        return event;
    }

    private User enabledUser(String email) {
        User user = new User();
        user.setEnabled(true);
        user.setEmail(email);
        return user;
    }
}
