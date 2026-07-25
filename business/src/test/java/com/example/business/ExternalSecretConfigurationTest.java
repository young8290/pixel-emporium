package com.example.business;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalSecretConfigurationTest {
    @Test
    void baseConfigurationRequiresExternalDatabaseAndOssSecrets() throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertTrue(yaml.contains("password: ${BUSINESS_DB_PASSWORD:}"));
        assertTrue(yaml.contains("access-key: ${OSS_ACCESS_KEY:}"));
        assertTrue(yaml.contains("secret-key: ${OSS_SECRET_KEY:}"));
        assertFalse(yaml.contains("password: xxx"));
        assertFalse(yaml.contains("your-access-key"));
        assertFalse(yaml.contains("your-secret-key"));
    }
}
