package com.example.user;

import com.example.common.exception.CustomValidationException;
import com.example.user.security.PasswordPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void rejectsShortCommonAndOverlongPlaintextPasswords() {
        assertThrows(CustomValidationException.class, () -> policy.validate("x"));
        assertThrows(CustomValidationException.class, () -> policy.validate("1234"));
        assertThrows(CustomValidationException.class, () -> policy.validate("password123"));
        assertThrows(CustomValidationException.class, () -> policy.validate("x".repeat(129)));
    }

    @Test
    void acceptsUnicodeAndPasswordManagerStylePasswords() {
        assertDoesNotThrow(() -> policy.validate("春风吹过像素农场2026"));
        assertDoesNotThrow(() -> policy.validate("correct horse battery staple ! 42"));
        assertDoesNotThrow(() -> policy.validate("x".repeat(128)));
    }
}
