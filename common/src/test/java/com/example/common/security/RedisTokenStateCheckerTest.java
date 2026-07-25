package com.example.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedisTokenStateCheckerTest {
    private static final String SECRET = "redis-token-state-test-secret-long-enough";

    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private RedisTokenStateChecker checker;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        checker = new RedisTokenStateChecker(redis);
    }

    @Test
    void acceptsOnlyMatchingSessionVersionWithUnrevokedJti() {
        String token = token(42L, "jti-1", 3L);
        when(redis.hasKey("auth:blacklist:jti-1")).thenReturn(false);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:token-version:42")).thenReturn("3");

        assertFalse(checker.isBlacklisted(token));
    }

    @Test
    void rejectsBlacklistedJtiBeforeVersionLookup() {
        String token = token(42L, "jti-2", 3L);
        when(redis.hasKey("auth:blacklist:jti-2")).thenReturn(true);

        assertTrue(checker.isBlacklisted(token));
        verifyNoInteractions(values);
    }

    @Test
    void rejectsMissingOrMismatchedSessionVersionAndLegacyClaims() {
        when(redis.hasKey("auth:blacklist:jti-3")).thenReturn(false);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:token-version:42")).thenReturn("4");

        assertTrue(checker.isBlacklisted(token(42L, "jti-3", 3L)));
        assertTrue(checker.isBlacklisted(tokenWithoutVersion()));
    }

    @Test
    void redisFailurePropagatesInsteadOfAllowingToken() {
        String token = token(42L, "jti-4", 3L);
        when(redis.hasKey("auth:blacklist:jti-4"))
                .thenThrow(new RedisConnectionFailureException("redis down"));

        assertThrows(RedisConnectionFailureException.class, () -> checker.isBlacklisted(token));
    }

    private String token(Long userId, String jti, Long version) {
        return JwtUtil.sign(Map.of(
                        JwtClaims.ID, userId,
                        JwtClaims.JTI, jti,
                        JwtClaims.TOKEN_VERSION, version),
                SECRET,
                new Date(System.currentTimeMillis() - 1_000),
                new Date(System.currentTimeMillis() + 60_000));
    }

    private String tokenWithoutVersion() {
        return JwtUtil.sign(Map.of(JwtClaims.ID, 42L, JwtClaims.JTI, "legacy-jti"),
                SECRET,
                new Date(System.currentTimeMillis() - 1_000),
                new Date(System.currentTimeMillis() + 60_000));
    }
}
