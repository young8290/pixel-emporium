package com.example.common.security;

public final class JwtClaims {
    public static final String ID = "id";
    public static final String EMAIL = "email";
    public static final String ROLES = "roles";
    public static final String PERMISSIONS = "permissions";
    public static final String JTI = "jti";
    public static final String TOKEN_VERSION = "ver";

    private JwtClaims() {
    }
}
