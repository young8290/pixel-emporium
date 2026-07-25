package com.example.gateway.config;

import com.example.common.security.JwtSecretPolicy;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtSecretStartupValidator {
    @Value("${auth.jwt.secret-key:}")
    private String secret;

    @PostConstruct
    void validate() {
        JwtSecretPolicy.requireValid(secret);
    }
}
