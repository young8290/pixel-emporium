package com.example.user;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLogMigrationTest {
    @Test
    void migrationCreatesQueryableUserOperationLog() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V16__operation_log.sql"));

        assertTrue(sql.contains("create table if not exists `operation_log`"));
        assertTrue(sql.contains("`action`"));
        assertTrue(sql.contains("`target_id`"));
        assertTrue(sql.contains("`result`"));
        assertTrue(sql.contains("idx_created_at"));
    }
}
