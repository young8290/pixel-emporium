package com.example.business.config;

import com.example.common.security.RedisTokenStateChecker;
import com.example.common.security.TokenBlacklistChecker;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class TokenStateConfigTest {
    @Test
    void businessServiceAlwaysProvidesRedisBackedTokenChecker() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class));
            context.register(TokenStateConfig.class);
            context.refresh();

            TokenBlacklistChecker checker = context.getBean(TokenBlacklistChecker.class);

            assertInstanceOf(RedisTokenStateChecker.class, checker);
        }
    }
}
