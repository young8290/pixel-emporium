package com.example.common.config;

import com.example.common.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public abstract class AbstractLoginWebConfig implements WebMvcConfigurer {

    @Autowired
    protected LoginInterceptor loginInterceptor;

    @Override
    public final void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(publicPaths());
    }

    protected abstract String[] publicPaths();
}
