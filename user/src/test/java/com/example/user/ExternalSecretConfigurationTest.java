package com.example.user;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalSecretConfigurationTest {
    @Test
    void baseConfigurationRequiresExternalDatabaseAndRsaSecrets() throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertTrue(yaml.contains("password: ${USER_DB_PASSWORD:}"));
        assertTrue(yaml.contains("private-key: ${RSA_PRIVATE_KEY:}"));
        assertFalse(yaml.contains("password: xxx"));
        assertFalse(yaml.contains("private-key: xxx"));
    }
}
