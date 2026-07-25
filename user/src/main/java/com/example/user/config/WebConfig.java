package com.example.user.config;

import com.example.common.config.AbstractLoginWebConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig extends AbstractLoginWebConfig {
    @Override
    protected String[] publicPaths() {
        return new String[]{
                "/auth/login",
                "/auth/admin/login",
                "/auth/register",
                "/auth/email-code",
                "/internal/mail/order-created",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/error",
                "/",
                "/hi"
        };
    }
}
