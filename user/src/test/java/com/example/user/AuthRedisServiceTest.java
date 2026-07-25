package com.example.user;

import com.example.common.exception.CustomUnauthorizedException;
import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import com.example.user.service.LoginAttemptService;
import com.example.user.service.TokenBlacklistService;
import com.example.user.service.impl.LoginAttemptServiceImpl;
import com.example.user.service.impl.TokenBlacklistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRedisServiceTest {
    private static final String SECRET = "auth-redis-service-test-secret";

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    private TokenBlacklistService tokenBlacklist;
    private LoginAttemptService loginAttempts;

    @BeforeEach
    void setUp() {
        tokenBlacklist = new TokenBlacklistServiceImpl(redis);
        loginAttempts = new LoginAttemptServiceImpl(redis);
    }

    @Test
    void blacklistStoresJtiForTokenRemainingLifetime() {
        when(redis.opsForValue()).thenReturn(values);
        String token = token(Map.of(JwtClaims.JTI, "jti-1"), 60_000);

        tokenBlacklist.blacklist(token);

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(values).set(org.mockito.ArgumentMatchers.eq("auth:blacklist:jti-1"),
                org.mockito.ArgumentMatchers.eq("1"), ttl.capture());
        assertTrue(ttl.getValue().toMillis() > 0);
        assertTrue(ttl.getValue().toMillis() <= 60_000);
    }

    @Test
    void blacklistDoesNotStoreLegacyTokenWithoutJti() {
        String token = token(Map.of(JwtClaims.ID, 42L), 60_000);

        tokenBlacklist.blacklist(token);

        verifyNoInteractions(redis);
    }

    @Test
    void blacklistDoesNotStoreExpiredToken() {
        String token = token(Map.of(JwtClaims.JTI, "expired-jti"), -1_000);

        tokenBlacklist.blacklist(token);

        verifyNoInteractions(redis);
    }

    @Test
    void blacklistIfNotBlacklistedUsesAtomicSetIfAbsent() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(org.mockito.ArgumentMatchers.eq("auth:blacklist:jti-atomic"),
                org.mockito.ArgumentMatchers.eq("1"), org.mockito.ArgumentMatchers.any(Duration.class)))
                .thenReturn(true);
        String token = token(Map.of(JwtClaims.JTI, "jti-atomic"), 60_000);

        assertTrue(tokenBlacklist.blacklistIfNotBlacklisted(token));
        verify(values).setIfAbsent(org.mockito.ArgumentMatchers.eq("auth:blacklist:jti-atomic"),
                org.mockito.ArgumentMatchers.eq("1"), org.mockito.ArgumentMatchers.any(Duration.class));
    }

    @Test
    void legacyTokenWithoutVersionIsRejectedWithoutRedisLookup() {
        String token = token(Map.of(JwtClaims.ID, 42L), 60_000);

        assertTrue(tokenBlacklist.isBlacklisted(token));

        verifyNoInteractions(redis);
    }

    @Test
    void tokenIsBlacklistedWhenItsJtiKeyExists() {
        when(redis.hasKey("auth:blacklist:jti-2")).thenReturn(true);
        String token = token(Map.of(
                JwtClaims.ID, 42L,
                JwtClaims.JTI, "jti-2",
                JwtClaims.TOKEN_VERSION, 3L), 60_000);

        assertTrue(tokenBlacklist.isBlacklisted(token));
    }

    @Test
    void matchingStoredVersionKeepsUnrevokedTokenActive() {
        when(redis.hasKey("auth:blacklist:jti-3")).thenReturn(false);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:token-version:42")).thenReturn("3");
        String token = token(Map.of(
                JwtClaims.ID, 42L,
                JwtClaims.JTI, "jti-3",
                JwtClaims.TOKEN_VERSION, 3L), 60_000);

        assertFalse(tokenBlacklist.isBlacklisted(token));
    }

    @Test
    void mismatchedStoredVersionRevokesExistingToken() {
        when(redis.hasKey("auth:blacklist:jti-4")).thenReturn(false);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:token-version:42")).thenReturn("4");
        String token = token(Map.of(
                JwtClaims.ID, 42L,
                JwtClaims.JTI, "jti-4",
                JwtClaims.TOKEN_VERSION, 3L), 60_000);

        assertTrue(tokenBlacklist.isBlacklisted(token));
    }

    @Test
    void staleTokenVersionCannotLowerRedisAndReturnsAuthoritativeVersion() {
        when(redis.execute(org.mockito.ArgumentMatchers.any(RedisScript.class),
                org.mockito.ArgumentMatchers.eq(java.util.List.of("auth:token-version:42")),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(1L, 1L);

        assertEquals(1L, tokenBlacklist.storeTokenVersion(42L, 1L));
        assertEquals(1L, tokenBlacklist.storeTokenVersion(42L, 0L));

        ArgumentCaptor<RedisScript<Long>> script = ArgumentCaptor.forClass(RedisScript.class);
        verify(redis, org.mockito.Mockito.times(2)).execute(script.capture(),
                org.mockito.ArgumentMatchers.eq(java.util.List.of("auth:token-version:42")),
                org.mockito.ArgumentMatchers.anyString());
        String lua = script.getAllValues().getFirst().getScriptAsString();
        assertTrue(lua.contains("redis.call('GET', KEYS[1])"));
        assertTrue(lua.contains("proposed > current"));
        verify(values, never()).set(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void tokenVersionStorePropagatesRedisFailure() {
        when(redis.execute(org.mockito.ArgumentMatchers.any(RedisScript.class),
                org.mockito.ArgumentMatchers.eq(java.util.List.of("auth:token-version:42")),
                org.mockito.ArgumentMatchers.eq("1")))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        assertThrows(RedisConnectionFailureException.class,
                () -> tokenBlacklist.storeTokenVersion(42L, 1L));
    }

    @Test
    void firstFailureUsesNormalizedEmailAndStartsTenMinuteWindow() {
        when(redis.execute(org.mockito.ArgumentMatchers.any(RedisScript.class),
                org.mockito.ArgumentMatchers.eq(java.util.List.of("auth:login:fail:test@example.com")),
                org.mockito.ArgumentMatchers.eq("600"))).thenReturn(1L);

        loginAttempts.onFailure(" Test@Example.COM ");

        ArgumentCaptor<RedisScript<Long>> script = ArgumentCaptor.forClass(RedisScript.class);
        verify(redis).execute(script.capture(),
                org.mockito.ArgumentMatchers.eq(java.util.List.of("auth:login:fail:test@example.com")),
                org.mockito.ArgumentMatchers.eq("600"));
        assertTrue(script.getValue().getScriptAsString().contains("TTL"));
    }

    @Test
    void laterFailuresDoNotRestartTheWindow() {
        when(redis.execute(org.mockito.ArgumentMatchers.any(RedisScript.class),
                org.mockito.ArgumentMatchers.eq(java.util.List.of("auth:login:fail:test@example.com")),
                org.mockito.ArgumentMatchers.eq("600"))).thenReturn(2L);

        loginAttempts.onFailure("test@example.com");

        verify(redis).execute(org.mockito.ArgumentMatchers.any(RedisScript.class),
                org.mockito.ArgumentMatchers.eq(java.util.List.of("auth:login:fail:test@example.com")),
                org.mockito.ArgumentMatchers.eq("600"));
    }

    @Test
    void fiveFailuresAreBlocked() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:login:fail:test@example.com")).thenReturn("5");

        assertTrue(loginAttempts.isBlocked("TEST@example.com"));
        assertThrows(CustomUnauthorizedException.class,
                () -> loginAttempts.assertAllowed("test@example.com"));
    }

    @Test
    void fewerThanFiveFailuresAreAllowed() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:login:fail:test@example.com")).thenReturn("4");

        assertFalse(loginAttempts.isBlocked("test@example.com"));
        loginAttempts.assertAllowed("test@example.com");
    }

    @Test
    void successfulLoginClearsNormalizedFailureKey() {
        loginAttempts.onSuccess(" Test@Example.COM ");

        verify(redis).delete("auth:login:fail:test@example.com");
    }

    @Test
    void thresholdAndWindowCanBeConfigured() {
        LoginAttemptServiceImpl configurable = new LoginAttemptServiceImpl(redis);
        ReflectionTestUtils.setField(configurable, "blockThreshold", 3L);
        ReflectionTestUtils.setField(configurable, "windowSeconds", 120L);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.execute(org.mockito.ArgumentMatchers.any(RedisScript.class),
                org.mockito.ArgumentMatchers.eq(java.util.List.of("auth:login:fail:test@example.com")),
                org.mockito.ArgumentMatchers.eq("120"))).thenReturn(1L);
        when(values.get("auth:login:fail:test@example.com")).thenReturn("3");

        configurable.onFailure("test@example.com");

        verify(redis).execute(org.mockito.ArgumentMatchers.any(RedisScript.class),
                org.mockito.ArgumentMatchers.eq(java.util.List.of("auth:login:fail:test@example.com")),
                org.mockito.ArgumentMatchers.eq("120"));
        assertTrue(configurable.isBlocked("test@example.com"));
    }

    private String token(Map<String, Object> claims, long expiresInMillis) {
        long now = System.currentTimeMillis();
        return JwtUtil.sign(claims, SECRET, new Date(now - 1_000), new Date(now + expiresInMillis));
    }
}
