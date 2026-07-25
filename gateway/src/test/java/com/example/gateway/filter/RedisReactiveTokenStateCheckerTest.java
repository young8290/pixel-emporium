package com.example.gateway.filter;

import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedisReactiveTokenStateCheckerTest {
    private static final String SECRET = "gateway-reactive-state-test-secret-long-enough";

    private ReactiveStringRedisTemplate redis;
    private ReactiveValueOperations<String, String> values;
    private RedisReactiveTokenStateChecker checker;

    @BeforeEach
    void setUp() {
        redis = mock(ReactiveStringRedisTemplate.class);
        values = mock(ReactiveValueOperations.class);
        checker = new RedisReactiveTokenStateChecker(redis);
    }

    @Test
    void acceptsMatchingVersionAndRejectsMismatch() {
        when(redis.hasKey("auth:blacklist:gateway-jti")).thenReturn(Mono.just(false));
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:token-version:42")).thenReturn(Mono.just("3"), Mono.just("4"));

        assertFalse(checker.isBlacklisted(token(3L)).block());
        assertTrue(checker.isBlacklisted(token(3L)).block());
    }

    @Test
    void rejectsRevokedJtiAndLegacyToken() {
        when(redis.hasKey("auth:blacklist:gateway-jti")).thenReturn(Mono.just(true));

        assertTrue(checker.isBlacklisted(token(3L)).block());
        assertTrue(checker.isBlacklisted(legacyToken()).block());
        verifyNoInteractions(values);
    }

    @Test
    void redisFailureRemainsAnErrorForFilterToFailClosed() {
        when(redis.hasKey("auth:blacklist:gateway-jti"))
                .thenReturn(Mono.error(new RedisConnectionFailureException("redis down")));

        assertThrows(RedisConnectionFailureException.class,
                () -> checker.isBlacklisted(token(3L)).block());
    }

    @Test
    void versionLookupFailureAlsoRemainsAnError() {
        when(redis.hasKey("auth:blacklist:gateway-jti")).thenReturn(Mono.just(false));
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:token-version:42"))
                .thenReturn(Mono.error(new RedisConnectionFailureException("redis down")));

        assertThrows(RedisConnectionFailureException.class,
                () -> checker.isBlacklisted(token(3L)).block());
    }

    private String token(Long version) {
        return JwtUtil.sign(Map.of(
                        JwtClaims.ID, 42L,
                        JwtClaims.JTI, "gateway-jti",
                        JwtClaims.TOKEN_VERSION, version),
                SECRET,
                new Date(System.currentTimeMillis() - 1_000),
                new Date(System.currentTimeMillis() + 60_000));
    }

    private String legacyToken() {
        return JwtUtil.sign(Map.of(JwtClaims.ID, 42L, JwtClaims.JTI, "legacy-jti"),
                SECRET,
                new Date(System.currentTimeMillis() - 1_000),
                new Date(System.currentTimeMillis() + 60_000));
    }
}
