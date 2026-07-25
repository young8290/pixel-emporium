package com.example.common.security;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "jwt-test-secret-which-is-long-enough";

    @Test
    void signThenValidateSucceeds() {
        Date issuedAt = new Date(System.currentTimeMillis() - 1_000);
        Date expiresAt = new Date(System.currentTimeMillis() + 60_000);
        String token = JwtUtil.sign(
                Map.of(JwtClaims.ID, 42L, JwtClaims.EMAIL, "user@example.com"),
                SECRET, issuedAt, expiresAt);

        assertTrue(JwtUtil.validate(token, SECRET));
        assertEquals(42L, ((Number) JwtUtil.getClaim(token, JwtClaims.ID)).longValue());
    }

    @Test
    void tamperedTokenFails() {
        Date issuedAt = new Date(System.currentTimeMillis() - 1_000);
        Date expiresAt = new Date(System.currentTimeMillis() + 60_000);
        String token = JwtUtil.sign(Map.of(JwtClaims.ID, 42L), SECRET, issuedAt, expiresAt);
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertFalse(JwtUtil.validate(tampered, SECRET));
    }

    @Test
    void expiredTokenFails() {
        Date issuedAt = new Date(System.currentTimeMillis() - 60_000);
        Date expiresAt = new Date(System.currentTimeMillis() - 1_000);
        String token = JwtUtil.sign(Map.of(JwtClaims.ID, 42L), SECRET, issuedAt, expiresAt);

        assertFalse(JwtUtil.validate(token, SECRET));
    }

    @Test
    void readsRolesJtiAndExpiration() {
        long now = System.currentTimeMillis();
        String token = JwtUtil.sign(
                Map.of(
                        JwtClaims.ID, 42L,
                        JwtClaims.JTI, "jti-1",
                        JwtClaims.ROLES, List.of("ADMIN", "USER")),
                SECRET,
                new Date(now - 1_000),
                new Date(now + 60_000));

        assertEquals(List.of("ADMIN", "USER"), JwtUtil.getRoles(token));
        assertEquals("jti-1", JwtUtil.getJti(token));
        assertTrue(JwtUtil.getExpirationMillis(token) > now);
    }

    @Test
    void legacyTokenWithoutJtiRemainsCompatible() {
        String token = JwtUtil.sign(
                Map.of(JwtClaims.ID, 42L),
                SECRET,
                new Date(System.currentTimeMillis() - 1_000),
                new Date(System.currentTimeMillis() + 60_000));

        assertNull(JwtUtil.getJti(token));
    }
}
