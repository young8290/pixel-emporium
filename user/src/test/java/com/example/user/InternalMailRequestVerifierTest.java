package com.example.user;

import com.example.user.config.MailCenterProperties;
import com.example.user.security.InternalMailRequestVerifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalMailRequestVerifierTest {
    @Test
    void signsRawBodyAndRejectsTampering() {
        var properties = new MailCenterProperties(null,
                new MailCenterProperties.Internal("01234567890123456789012345678901", 300), null, null);
        var verifier = new InternalMailRequestVerifier(properties);
        byte[] body = "{\"eventId\":\"ORDER_CREATED:1\"}".getBytes();
        long timestamp = java.time.Instant.now().getEpochSecond();
        String signature = verifier.sign(timestamp, "ORDER_CREATED:1", body);
        assertDoesNotThrow(() -> verifier.verify(timestamp, "ORDER_CREATED:1", signature, body));
        assertThrows(RuntimeException.class, () -> verifier.verify(timestamp, "ORDER_CREATED:1", signature, "{}".getBytes()));
    }

    @Test
    void rejectsExpiredRequestsAndUnavailableSecrets() {
        var properties = new MailCenterProperties(null,
                new MailCenterProperties.Internal("01234567890123456789012345678901", 300), null, null);
        var verifier = new InternalMailRequestVerifier(properties);
        byte[] body = "{}".getBytes();
        long expired = java.time.Instant.now().minusSeconds(301).getEpochSecond();
        String signature = verifier.sign(expired, "ORDER_CREATED:1", body);

        assertThrows(RuntimeException.class, () -> verifier.verify(expired, "ORDER_CREATED:1", signature, body));
        var unavailable = new InternalMailRequestVerifier(new MailCenterProperties(null,
                new MailCenterProperties.Internal("short", 300), null, null));
        assertThrows(RuntimeException.class, () -> unavailable.verify(java.time.Instant.now().getEpochSecond(),
                "ORDER_CREATED:1", "00", body));
    }
}
