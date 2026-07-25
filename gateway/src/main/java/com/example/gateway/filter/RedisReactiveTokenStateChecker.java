package com.example.gateway.filter;

import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import com.example.common.security.TokenStateKeys;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RedisReactiveTokenStateChecker implements ReactiveTokenStateChecker {
    private final ReactiveStringRedisTemplate redis;

    public RedisReactiveTokenStateChecker(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Boolean> isBlacklisted(String token) {
        Long userId = longClaim(JwtUtil.getClaim(token, JwtClaims.ID));
        Long tokenVersion = longClaim(JwtUtil.getClaim(token, JwtClaims.TOKEN_VERSION));
        String jti = JwtUtil.getJti(token);
        if (userId == null || tokenVersion == null || jti == null || jti.isBlank()) {
            return Mono.just(true);
        }

        return redis.hasKey(TokenStateKeys.blacklist(jti))
                .flatMap(revoked -> {
                    if (Boolean.TRUE.equals(revoked)) {
                        return Mono.just(true);
                    }
                    return redis.opsForValue().get(TokenStateKeys.version(userId))
                            .map(current -> !String.valueOf(tokenVersion).equals(current))
                            .defaultIfEmpty(true);
                });
    }

    private Long longClaim(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
