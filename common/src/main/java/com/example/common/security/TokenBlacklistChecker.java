package com.example.common.security;

public interface TokenBlacklistChecker {
    boolean isBlacklisted(String token);
}
