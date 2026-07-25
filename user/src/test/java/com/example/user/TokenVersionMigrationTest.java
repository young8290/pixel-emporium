package com.example.user;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenVersionMigrationTest {
    @Test
    void migrationAddsNonNullMonotonicTokenVersion() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V15__user_token_version.sql"));

        assertTrue(sql.toLowerCase().contains("add column token_version bigint not null default 0"));
    }
}
