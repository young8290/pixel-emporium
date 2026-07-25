package com.example.business;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationOutboxMigrationTest {
    @Test
    void notificationOutboxMigrationHasUniqueEventAndDueIndex() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V13__notification_outbox.sql"));
        assertTrue(sql.contains("create table if not exists notification_outbox"));
        assertTrue(sql.contains("unique key uk_notification_event"));
        assertTrue(sql.contains("idx_notification_due"));
    }
}
