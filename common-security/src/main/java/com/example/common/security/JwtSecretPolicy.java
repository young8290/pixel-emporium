package com.example.common.security;

import java.nio.charset.StandardCharsets;

/** Validates the shared signing secret before a service starts accepting tokens. */
public final class JwtSecretPolicy {
    private static final int MIN_BYTES = 32;

    private JwtSecretPolicy() {
    }

    public static void requireValid(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        String value = secret.trim();
        if (value.length() != secret.length()
                || value.chars().anyMatch(Character::isWhitespace)
                || value.getBytes(StandardCharsets.UTF_8).length < MIN_BYTES) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 non-whitespace bytes");
        }
        if (value.contains("不能提交到代码仓库")
                || value.contains("至少几十个字符")
                || value.contains("JWT_SECRET")) {
            throw new IllegalStateException("JWT_SECRET must not use a committed example value");
        }
        long distinct = value.codePoints().distinct().count();
        if (distinct < 8) {
            throw new IllegalStateException("JWT_SECRET has insufficient character diversity");
        }
    }
}
