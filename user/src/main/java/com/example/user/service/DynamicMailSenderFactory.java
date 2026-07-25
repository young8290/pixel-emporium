package com.example.user.service;

import com.example.user.entity.MailConfig;
import com.example.user.enums.MailSecurityMode;
import com.example.user.security.MailSecretCipher;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.Properties;

@Component
public class DynamicMailSenderFactory {
    private final MailSecretCipher cipher; private volatile Integer cachedVersion; private volatile JavaMailSenderImpl cached;
    public DynamicMailSenderFactory(MailSecretCipher cipher) { this.cipher = cipher; }
    public JavaMailSenderImpl create(MailConfig config) {
        if (cached != null && java.util.Objects.equals(cachedVersion, config.getVersion())) return cached;
        synchronized (this) { if (cached != null && java.util.Objects.equals(cachedVersion, config.getVersion())) return cached;
            JavaMailSenderImpl sender = new JavaMailSenderImpl(); sender.setHost(config.getHost()); sender.setPort(config.getPort()); sender.setUsername(config.getUsername());
            if (StringUtils.hasText(config.getPasswordCiphertext())) sender.setPassword(cipher.decryptSmtpPassword(config.getPasswordCiphertext()));
            Properties p = sender.getJavaMailProperties(); p.put("mail.smtp.auth", String.valueOf(StringUtils.hasText(config.getUsername())));
            p.put("mail.smtp.connectiontimeout", String.valueOf(config.getConnectionTimeoutMs())); p.put("mail.smtp.timeout", String.valueOf(config.getReadTimeoutMs())); p.put("mail.smtp.writetimeout", String.valueOf(config.getReadTimeoutMs()));
            p.put("mail.smtp.starttls.enable", String.valueOf(config.getSecurityMode() == MailSecurityMode.STARTTLS)); p.put("mail.smtp.starttls.required", String.valueOf(config.getSecurityMode() == MailSecurityMode.STARTTLS)); p.put("mail.smtp.ssl.enable", String.valueOf(config.getSecurityMode() == MailSecurityMode.SSL_TLS));
            cached = sender; cachedVersion = config.getVersion(); return sender;
        }
    }
    public void clear() { cached = null; cachedVersion = null; }
}
