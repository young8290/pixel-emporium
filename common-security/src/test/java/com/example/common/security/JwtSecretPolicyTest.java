package com.example.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretPolicyTest {
    @Test
    void rejectsBlankShortAndCommittedExampleSecrets() {
        assertThrows(IllegalStateException.class, () -> JwtSecretPolicy.requireValid(null));
        assertThrows(IllegalStateException.class, () -> JwtSecretPolicy.requireValid("short-secret"));
        assertThrows(IllegalStateException.class, () -> JwtSecretPolicy.requireValid(
                "这是一串密钥,正常应该长一点，至少几十个字符，不能泄露，不能提交到代码仓库，在-dev配置文件覆盖这个值"));
    }

    @Test
    void acceptsAHighEntropyConfiguredSecret() {
        assertDoesNotThrow(() -> JwtSecretPolicy.requireValid(
                "q7V!m2R#x9P@k4L$w8N^c6T&y3H*e5Z%"));
    }
}
