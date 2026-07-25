package com.example.common.security;

import cn.hutool.core.convert.Convert;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisTokenStateChecker implements TokenBlacklistChecker {
    private final StringRedisTemplate redis;

    public RedisTokenStateChecker(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean isBlacklisted(String token) {
        Long userId = Convert.toLong(JwtUtil.getClaim(token, JwtClaims.ID), null);
        Long tokenVersion = Convert.toLong(JwtUtil.getClaim(token, JwtClaims.TOKEN_VERSION), null);
        String jti = JwtUtil.getJti(token);
        if (userId == null || tokenVersion == null || jti == null || jti.isBlank()) {
            return true;
        }

        Boolean jtiRevoked = redis.hasKey(TokenStateKeys.blacklist(jti));
        if (!Boolean.FALSE.equals(jtiRevoked)) {
            return true;
        }

        String currentVersion = redis.opsForValue().get(TokenStateKeys.version(userId));
        return !String.valueOf(tokenVersion).equals(currentVersion);
    }
}
