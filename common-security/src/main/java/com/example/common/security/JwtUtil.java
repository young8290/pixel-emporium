package com.example.common.security;

import cn.hutool.core.convert.Convert;
import cn.hutool.jwt.JWT;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class JwtUtil {

    private JwtUtil() {
    }

    public static String sign(Map<String, Object> claims, String secret, Date issuedAt, Date expiresAt) {
        JWT jwt = JWT.create();
        if (claims != null) {
            for (Map.Entry<String, Object> entry : claims.entrySet()) {
                jwt.setPayload(entry.getKey(), entry.getValue());
            }
        }
        return jwt.setIssuedAt(issuedAt)
                .setExpiresAt(expiresAt)
                .setKey(key(secret))
                .sign();
    }

    public static boolean validate(String token, String secret) {
        if (token == null || token.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }
        try {
            return JWT.of(token).setKey(key(secret)).validate(0);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static Object getClaim(String token, String key) {
        return JWT.of(token).getPayload(key);
    }

    public static String getJti(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Object value = getClaim(token, JwtClaims.JTI);
            return value == null ? null : String.valueOf(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static Long getExpirationMillis(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Object value = getClaim(token, "exp");
            if (value instanceof Date date) {
                return date.getTime();
            }
            Long numeric = Convert.toLong(value);
            if (numeric == null) {
                return null;
            }
            return numeric < 10_000_000_000L ? numeric * 1000L : numeric;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static List<String> getRoles(String token) {
        List<String> roles = Convert.toList(String.class, getClaim(token, JwtClaims.ROLES));
        return roles == null ? Collections.emptyList() : roles;
    }

    private static byte[] key(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be blank");
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
