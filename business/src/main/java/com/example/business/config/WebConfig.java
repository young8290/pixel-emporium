package com.example.business.config;

import com.example.common.config.AbstractLoginWebConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig extends AbstractLoginWebConfig {
    @Override
    protected String[] publicPaths() {
        return new String[]{
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/error",
                "/hi",
                "/category/tree",
                "/product/**",
                "/pay/alipay/notify"
        };
    }
}
