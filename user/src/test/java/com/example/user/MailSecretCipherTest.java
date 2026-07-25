package com.example.user;

import com.example.common.exception.CustomBusinessException;
import com.example.user.config.MailCenterProperties;
import com.example.user.security.MailSecretCipher;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class MailSecretCipherTest {
    private final MailSecretCipher cipher = new MailSecretCipher(new MailCenterProperties(
            new MailCenterProperties.Crypto(Base64.getEncoder().encodeToString(new byte[32])),
            null, null, null));

    @Test
    void encryptsWithDifferentIvsAndRejectsWrongPurpose() {
        String first = cipher.encryptSmtpPassword("smtp-secret");
        String second = cipher.encryptSmtpPassword("smtp-secret");
        assertNotEquals(first, second);
        assertEquals("smtp-secret", cipher.decryptSmtpPassword(first));
        assertThrows(CustomBusinessException.class, () -> cipher.decryptDeliveryModel(first));
    }

    @Test
    void rejectsTamperedCiphertextAndMissingOrWrongLengthKeys() {
        String encrypted = cipher.encryptDeliveryModel("event");
        String tampered = encrypted.substring(0, encrypted.length() - 1)
                + (encrypted.endsWith("A") ? "B" : "A");
        assertThrows(CustomBusinessException.class, () -> cipher.decryptDeliveryModel(tampered));
        assertThrows(CustomBusinessException.class, () -> new MailSecretCipher(new MailCenterProperties(
                new MailCenterProperties.Crypto(""), null, null, null)).encryptSmtpPassword("x"));
        assertThrows(CustomBusinessException.class, () -> new MailSecretCipher(new MailCenterProperties(
                new MailCenterProperties.Crypto(Base64.getEncoder().encodeToString(new byte[31])), null, null, null))
                .encryptSmtpPassword("x"));
    }
}
