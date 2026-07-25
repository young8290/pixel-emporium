package com.example.business.config;

import com.example.common.security.RedisTokenStateChecker;
import com.example.common.security.TokenBlacklistChecker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class TokenStateConfig {
    @Bean
    public TokenBlacklistChecker tokenBlacklistChecker(StringRedisTemplate redis) {
        return new RedisTokenStateChecker(redis);
    }
}
