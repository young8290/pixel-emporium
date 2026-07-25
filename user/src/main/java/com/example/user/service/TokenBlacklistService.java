package com.example.user.service;

import com.example.common.security.TokenBlacklistChecker;

public interface TokenBlacklistService extends TokenBlacklistChecker {
    void blacklist(String token);

    boolean blacklistIfNotBlacklisted(String token);

    long storeTokenVersion(Long userId, long tokenVersion);
}
