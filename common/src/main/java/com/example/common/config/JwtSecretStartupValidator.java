package com.example.common.config;

import com.example.common.security.JwtSecretPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class JwtSecretStartupValidator {
    @Value("${auth.jwt.secret-key:}")
    private String secret;

    @PostConstruct
    void validate() {
        JwtSecretPolicy.requireValid(secret);
    }
}
