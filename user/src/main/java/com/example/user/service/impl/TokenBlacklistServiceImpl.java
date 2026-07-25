package com.example.user.service.impl;

import com.example.common.security.JwtUtil;
import com.example.common.security.RedisTokenStateChecker;
import com.example.common.security.TokenStateKeys;
import com.example.user.service.TokenBlacklistService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {
    private static final RedisScript<Long> STORE_TOKEN_VERSION_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            local proposed = tonumber(ARGV[1])
            if not proposed then
                return redis.error_reply('invalid proposed token version')
            end
            if not current then
                redis.call('SET', KEYS[1], ARGV[1])
                return proposed
            end
            local currentNumber = tonumber(current)
            if not currentNumber then
                return redis.error_reply('invalid stored token version')
            end
            if proposed > currentNumber then
                redis.call('SET', KEYS[1], ARGV[1])
                return proposed
            end
            return currentNumber
            """, Long.class);

    private final StringRedisTemplate redis;
    private final RedisTokenStateChecker tokenStateChecker;

    public TokenBlacklistServiceImpl(StringRedisTemplate redis) {
        this.redis = redis;
        this.tokenStateChecker = new RedisTokenStateChecker(redis);
    }

    @Override
    public void blacklist(String token) {
        TokenData tokenData = tokenData(token);
        if (tokenData == null) {
            return;
        }
        redis.opsForValue().set(tokenData.key(), "1", tokenData.ttl());
    }

    @Override
    public boolean blacklistIfNotBlacklisted(String token) {
        TokenData tokenData = tokenData(token);
        if (tokenData == null) {
            return false;
        }
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(
                tokenData.key(), "1", tokenData.ttl()));
    }

    @Override
    public boolean isBlacklisted(String token) {
        return tokenStateChecker.isBlacklisted(token);
    }

    @Override
    public long storeTokenVersion(Long userId, long tokenVersion) {
        if (userId == null || tokenVersion < 0) {
            throw new IllegalArgumentException("用户 ID 和令牌版本必须有效");
        }
        Long authoritativeVersion = redis.execute(STORE_TOKEN_VERSION_SCRIPT,
                List.of(TokenStateKeys.version(userId)), String.valueOf(tokenVersion));
        if (authoritativeVersion == null || authoritativeVersion < 0) {
            throw new IllegalStateException("用户会话版本同步失败");
        }
        return authoritativeVersion;
    }

    private TokenData tokenData(String token) {
        String jti = JwtUtil.getJti(token);
        Long expirationMillis = JwtUtil.getExpirationMillis(token);
        if (jti == null || jti.isBlank() || expirationMillis == null) {
            return null;
        }

        long remainingMillis = expirationMillis - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            return null;
        }
        return new TokenData(TokenStateKeys.blacklist(jti), Duration.ofMillis(remainingMillis));
    }

    private record TokenData(String key, Duration ttl) {
    }
}
