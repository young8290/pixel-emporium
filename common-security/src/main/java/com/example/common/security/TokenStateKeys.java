package com.example.common.security;

public final class TokenStateKeys {
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String VERSION_PREFIX = "auth:token-version:";

    private TokenStateKeys() {
    }

    public static String blacklist(String jti) {
        return BLACKLIST_PREFIX + jti;
    }

    public static String version(Long userId) {
        return VERSION_PREFIX + userId;
    }
}
