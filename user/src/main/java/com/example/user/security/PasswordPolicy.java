package com.example.user.security;

import com.example.common.exception.CustomValidationException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class PasswordPolicy {
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 128;
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "1234567890", "123456789", "password", "password123",
            "qwerty123", "admin123", "abc123456", "1111111111"
    );

    public void validate(String rawPassword) {
        if (rawPassword == null) {
            throw new CustomValidationException("密码不能为空");
        }
        int length = rawPassword.codePointCount(0, rawPassword.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new CustomValidationException("密码长度必须为10至128个字符");
        }
        if (COMMON_PASSWORDS.contains(rawPassword.toLowerCase(Locale.ROOT))) {
            throw new CustomValidationException("密码过于常见，请更换密码");
        }
    }
}
