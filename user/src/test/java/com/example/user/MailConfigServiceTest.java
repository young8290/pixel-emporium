package com.example.user;

import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomValidationException;
import com.example.user.entity.MailConfig;
import com.example.user.enums.MailSecurityMode;
import com.example.user.mapper.MailConfigMapper;
import com.example.user.request.MailConfigUpdateRequest;
import com.example.user.security.MailSecretCipher;
import com.example.user.service.DynamicMailSenderFactory;
import com.example.user.service.MailConfigService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MailConfigServiceTest {
    @Test
    void blankPasswordRetainsExistingCiphertext() {
        MailConfigMapper mapper = mock(MailConfigMapper.class);
        MailSecretCipher cipher = mock(MailSecretCipher.class);
        MailConfig current = config(3);
        current.setPasswordCiphertext("existing-ciphertext");
        when(mapper.selectById(1L)).thenReturn(current);
        when(mapper.update(any(MailConfig.class), any())).thenReturn(1);
        MailConfigService service = new MailConfigService(mapper, cipher, mock(DynamicMailSenderFactory.class));

        service.update(request(3));

        assertEquals("existing-ciphertext", current.getPasswordCiphertext());
        verify(cipher, never()).encryptSmtpPassword(anyString());
    }

    @Test
    void staleVersionIsRejectedBeforeUpdatingConfiguration() {
        MailConfigMapper mapper = mock(MailConfigMapper.class);
        when(mapper.selectById(1L)).thenReturn(config(3));
        MailConfigService service = new MailConfigService(mapper, mock(MailSecretCipher.class),
                mock(DynamicMailSenderFactory.class));

        assertThrows(CustomConflictException.class, () -> service.update(request(2)));
        verify(mapper, never()).update(any(MailConfig.class), any());
    }

    @Test
    void enabledConfigurationRequiresConnectionIdentityAndInitialPassword() {
        MailConfigMapper mapper = mock(MailConfigMapper.class);
        when(mapper.selectById(1L)).thenReturn(config(3));
        MailConfigService service = new MailConfigService(mapper, mock(MailSecretCipher.class),
                mock(DynamicMailSenderFactory.class));
        MailConfigUpdateRequest request = request(3);
        request.setEnabled(true);

        assertThrows(CustomValidationException.class, () -> service.update(request));
        verify(mapper, never()).update(any(MailConfig.class), any());
    }

    @Test
    void databaseOptimisticLockConflictIsReportedAndDoesNotClearSenderCache() {
        MailConfigMapper mapper = mock(MailConfigMapper.class);
        DynamicMailSenderFactory senderFactory = mock(DynamicMailSenderFactory.class);
        when(mapper.selectById(1L)).thenReturn(config(3));
        when(mapper.update(any(MailConfig.class), any())).thenReturn(0);
        MailConfigService service = new MailConfigService(mapper, mock(MailSecretCipher.class), senderFactory);

        assertThrows(CustomConflictException.class, () -> service.update(request(3)));

        verify(senderFactory, never()).clear();
    }

    @Test
    void newPasswordIsEncryptedAndSuccessfulUpdateClearsCachedSender() {
        MailConfigMapper mapper = mock(MailConfigMapper.class);
        MailSecretCipher cipher = mock(MailSecretCipher.class);
        DynamicMailSenderFactory senderFactory = mock(DynamicMailSenderFactory.class);
        MailConfig current = config(3);
        when(mapper.selectById(1L)).thenReturn(current);
        when(mapper.update(any(MailConfig.class), any())).thenReturn(1);
        when(cipher.encryptSmtpPassword("new-secret")).thenReturn("new-ciphertext");
        MailConfigService service = new MailConfigService(mapper, cipher, senderFactory);
        MailConfigUpdateRequest request = request(3);
        request.setPassword(" new-secret ");

        service.update(request);

        assertEquals("new-ciphertext", current.getPasswordCiphertext());
        verify(senderFactory).clear();
    }

    @Test
    void safeResponseExposesOnlyPasswordConfiguredFlag() {
        MailConfigMapper mapper = mock(MailConfigMapper.class);
        MailConfig current = config(3);
        current.setPasswordCiphertext("secret-ciphertext");
        when(mapper.selectById(1L)).thenReturn(current);
        MailConfigService service = new MailConfigService(mapper, mock(MailSecretCipher.class),
                mock(DynamicMailSenderFactory.class));

        assertTrue(service.get().getPasswordConfigured());
        current.setPasswordCiphertext("");
        assertFalse(service.get().getPasswordConfigured());
    }

    private MailConfig config(int version) {
        MailConfig config = new MailConfig();
        config.setId(1L);
        config.setVersion(version);
        config.setEnabled(false);
        config.setSecurityMode(MailSecurityMode.STARTTLS);
        return config;
    }

    private MailConfigUpdateRequest request(int version) {
        MailConfigUpdateRequest request = new MailConfigUpdateRequest();
        request.setVersion(version);
        request.setEnabled(false);
        request.setHost("");
        request.setPort(587);
        request.setSecurityMode(MailSecurityMode.STARTTLS);
        request.setUsername("");
        request.setPassword("");
        request.setFromAddress("");
        request.setFromName("");
        request.setReplyTo("");
        request.setConnectionTimeoutMs(5_000);
        request.setReadTimeoutMs(10_000);
        return request;
    }
}
