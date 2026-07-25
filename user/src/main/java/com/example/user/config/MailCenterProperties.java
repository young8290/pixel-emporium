package com.example.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "mail")
public record MailCenterProperties(Crypto crypto, Internal internal, Verification verification, Links links) {
    public record Crypto(String masterKeyBase64) {}
    public record Internal(String hmacSecret, long maxClockSkewSeconds) {}
    public record Verification(String pepper, long ttlSeconds, long resendCooldownSeconds,
                               int emailHourlyLimit, int ipHourlyLimit, int maxAttempts,
                               List<String> trustedProxyAddresses) {}
    public record Links(String storefrontBaseUrl, String assetBaseUrl) {}
}
