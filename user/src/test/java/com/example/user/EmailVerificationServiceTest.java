package com.example.user;

import com.example.user.config.MailCenterProperties;
import com.example.user.entity.User;
import com.example.user.service.EmailVerificationService;
import com.example.user.service.MailConfigService;
import com.example.user.service.MailTemplateService;
import com.example.user.service.UserService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {
    @Test
    void normalizesEmailAndDoesNotSendForAnExistingAccount() {
        UserService users = mock(UserService.class);
        when(users.getOne(any())).thenReturn(new User());
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        MailConfigService config = mock(MailConfigService.class);
        MailTemplateService templates = mock(MailTemplateService.class);
        EmailVerificationService service = new EmailVerificationService(redis, users, config, templates, properties());
        assertEquals("owner@example.com", service.normalizeEmail(" Owner@Example.COM "));
        assertEquals(60, service.sendRegistrationCode(" Owner@Example.COM ", "127.0.0.1").cooldownSeconds());
        verifyNoInteractions(redis, config, templates);
    }

    @Test
    void rateLimitScriptChecksBothCountersBeforeIncrementingAndMapsCooldownAndLimitResponses() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        UserService users = mock(UserService.class);
        when(users.getOne(any())).thenReturn(null);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any())).thenReturn(0L, -1L);
        EmailVerificationService service = service(redis, users, mock(MailConfigService.class), mock(MailTemplateService.class));

        assertThrows(com.example.common.exception.CustomValidationException.class, () -> service.sendRegistrationCode("buyer@example.com", "127.0.0.1"));
        assertThrows(com.example.common.exception.CustomValidationException.class, () -> service.sendRegistrationCode("buyer@example.com", "127.0.0.1"));

        org.mockito.ArgumentCaptor<DefaultRedisScript> script = org.mockito.ArgumentCaptor.forClass(DefaultRedisScript.class);
        verify(redis, times(2)).execute(script.capture(), anyList(), any(), any(), any());
        String lua = script.getValue().getScriptAsString();
        assertTrue(lua.indexOf("GET',KEYS[2]") < lua.indexOf("INCR',KEYS[2]"));
        assertTrue(lua.indexOf("GET',KEYS[3]") < lua.indexOf("INCR',KEYS[3]"));
    }

    @Test
    void sendsMultipartTextAndHtmlAndCleansCodeAndCooldownWhenSmtpFails() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any())).thenReturn(1L, 1L);
        UserService users = mock(UserService.class); when(users.getOne(any())).thenReturn(null);
        MailConfigService config = mock(MailConfigService.class);
        JavaMailSenderImpl sender = mock(JavaMailSenderImpl.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(config.sender()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(message);
        com.example.user.entity.MailConfig smtp = new com.example.user.entity.MailConfig(); smtp.setFromAddress("shop@example.com"); smtp.setFromName("Pixel Shop");
        when(config.current()).thenReturn(smtp);
        MailTemplateService templates = mock(MailTemplateService.class);
        when(templates.verification(any())).thenReturn(new MailTemplateService.RenderedMail("subject", "<p>html</p>", "text"));
        EmailVerificationService service = service(redis, users, config, templates);

        assertEquals(60, service.sendRegistrationCode("buyer@example.com", "127.0.0.1").cooldownSeconds());
        assertTrue(message.getContent() instanceof jakarta.mail.Multipart);

        doThrow(new RuntimeException("smtp down")).when(sender).send(any(MimeMessage.class));
        assertThrows(com.example.common.exception.CustomBusinessException.class, () -> service.sendRegistrationCode("other@example.com", "127.0.0.1"));
        org.mockito.ArgumentCaptor<List<String>> deleted = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(redis, atLeastOnce()).delete(deleted.capture());
        assertTrue(deleted.getAllValues().stream().anyMatch(keys -> keys.stream().anyMatch(key -> key.contains(":code:")) && keys.stream().anyMatch(key -> key.contains(":cooldown:"))));
    }

    @Test
    void validatesAttemptsAndConsumesAfterTooManyIncorrectCodes() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        String email = "buyer@example.com";
        String emailHash = sha256(email);
        String codeKey = "mail:verify:register:code:{" + emailHash + "}";
        String attemptsKey = "mail:verify:register:attempts:{" + emailHash + "}";
        when(values.get(codeKey)).thenReturn(sha256(email + "\nREGISTER\n123456\n" + properties().verification().pepper()));
        when(values.increment(attemptsKey)).thenReturn(1L, 6L);
        EmailVerificationService service = service(redis, mock(UserService.class), mock(MailConfigService.class), mock(MailTemplateService.class));

        assertDoesNotThrow(() -> service.assertValid(email, "123456"));
        assertThrows(com.example.common.exception.CustomValidationException.class, () -> service.assertValid(email, "000000"));

        verify(redis).expire(attemptsKey, Duration.ofSeconds(600));
        verify(redis).delete(List.of(codeKey, attemptsKey));
    }

    private EmailVerificationService service(StringRedisTemplate redis, UserService users, MailConfigService config, MailTemplateService templates) {
        return new EmailVerificationService(redis, users, config, templates, properties());
    }

    private String sha256(String input) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new AssertionError(exception); }
    }

    private MailCenterProperties properties() {
        return new MailCenterProperties(null, null,
                new MailCenterProperties.Verification("01234567890123456789012345678901", 600, 60, 5, 10, 5, List.of()), null);
    }
}
