package com.example.user;

import com.example.user.entity.MailConfig;
import com.example.user.enums.MailSecurityMode;
import com.example.user.security.MailSecretCipher;
import com.example.user.service.DynamicMailSenderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicMailSenderFactoryTest {
    @Test
    void appliesTimeoutAuthenticationAndTlsModesAndCachesByVersion() {
        MailSecretCipher cipher = mock(MailSecretCipher.class);
        when(cipher.decryptSmtpPassword("ciphertext")).thenReturn("secret");
        DynamicMailSenderFactory factory = new DynamicMailSenderFactory(cipher);

        MailConfig startTls = config(MailSecurityMode.STARTTLS, 1);
        JavaMailSenderImpl first = factory.create(startTls);

        assertEquals("smtp.example.com", first.getHost());
        assertEquals(587, first.getPort());
        assertEquals("secret", first.getPassword());
        assertEquals("4321", first.getJavaMailProperties().getProperty("mail.smtp.connectiontimeout"));
        assertEquals("8765", first.getJavaMailProperties().getProperty("mail.smtp.timeout"));
        assertEquals("8765", first.getJavaMailProperties().getProperty("mail.smtp.writetimeout"));
        assertTrue(Boolean.parseBoolean(first.getJavaMailProperties().getProperty("mail.smtp.auth")));
        assertTrue(Boolean.parseBoolean(first.getJavaMailProperties().getProperty("mail.smtp.starttls.enable")));
        assertFalse(Boolean.parseBoolean(first.getJavaMailProperties().getProperty("mail.smtp.ssl.enable")));
        assertSame(first, factory.create(startTls));

        MailConfig ssl = config(MailSecurityMode.SSL_TLS, 2);
        JavaMailSenderImpl second = factory.create(ssl);
        assertTrue(Boolean.parseBoolean(second.getJavaMailProperties().getProperty("mail.smtp.ssl.enable")));
        assertFalse(Boolean.parseBoolean(second.getJavaMailProperties().getProperty("mail.smtp.starttls.enable")));

        MailConfig none = config(MailSecurityMode.NONE, 3);
        none.setUsername("");
        JavaMailSenderImpl third = factory.create(none);
        assertFalse(Boolean.parseBoolean(third.getJavaMailProperties().getProperty("mail.smtp.auth")));
        assertFalse(Boolean.parseBoolean(third.getJavaMailProperties().getProperty("mail.smtp.ssl.enable")));
        assertFalse(Boolean.parseBoolean(third.getJavaMailProperties().getProperty("mail.smtp.starttls.enable")));
    }

    private MailConfig config(MailSecurityMode mode, int version) {
        MailConfig config = new MailConfig();
        config.setHost("smtp.example.com");
        config.setPort(587);
        config.setUsername("account@example.com");
        config.setPasswordCiphertext("ciphertext");
        config.setSecurityMode(mode);
        config.setConnectionTimeoutMs(4_321);
        config.setReadTimeoutMs(8_765);
        config.setVersion(version);
        return config;
    }
}
