package com.example.user;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class MailCenterMigrationTest {
    @Test
    void mailCenterMigrationStoresOnlyCiphertextAndSupportsIdempotency() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V10__mail_center.sql"));
        assertTrue(sql.contains("create table if not exists mail_config"));
        assertTrue(sql.contains("password_ciphertext"));
        assertFalse(Pattern.compile("`password`\\s", Pattern.CASE_INSENSITIVE).matcher(sql).find());
        assertTrue(sql.contains("create table if not exists mail_delivery"));
        assertTrue(sql.contains("unique key uk_mail_delivery_event"));
        assertTrue(sql.contains("next_retry_at"));
        assertTrue(sql.contains("insert into mail_config"));
    }
}
