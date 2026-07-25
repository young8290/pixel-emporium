package com.example.user.service.impl;

import com.example.common.exception.CustomUnauthorizedException;
import com.example.user.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {
    private static final String KEY_PREFIX = "auth:login:fail:";
    private final StringRedisTemplate redis;
    private static final DefaultRedisScript<Long> FAILURE_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) "
                    + "local ttl = redis.call('TTL', KEYS[1]) "
                    + "if count == 1 or ttl < 0 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return count", Long.class);

    @Value("${auth.login.max-failures:5}")
    private long blockThreshold = 5L;

    @Value("${auth.login.lock-duration-seconds:600}")
    private long windowSeconds = 600L;

    public LoginAttemptServiceImpl(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void onFailure(String email) {
        String key = key(email);
        redis.execute(FAILURE_SCRIPT, List.of(key), String.valueOf(windowSeconds));
    }

    @Override
    public void onSuccess(String email) {
        redis.delete(key(email));
    }

    @Override
    public boolean isBlocked(String email) {
        String count = redis.opsForValue().get(key(email));
        if (count == null) {
            return false;
        }
        try {
            return Long.parseLong(count) >= blockThreshold;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Override
    public void assertAllowed(String email) {
        if (isBlocked(email)) {
            throw new CustomUnauthorizedException("登录失败次数过多，请稍后重试");
        }
    }

    private String key(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return KEY_PREFIX + normalized;
    }
}
