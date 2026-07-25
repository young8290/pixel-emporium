package com.example.user;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MailDeliveryMapperSqlTest {

    @Test
    void stateTransitionsExplicitlyClearNullableDeliveryMetadata() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/example/user/mapper/MailDeliveryMapper.java"))
                .replaceAll("\\s+", " ")
                .toLowerCase();

        assertTrue(source.contains("int marksent("), "missing explicit SENT transition");
        assertTrue(source.contains("status = 'sent'"), "SENT transition must write the state");
        assertTrue(source.contains("claimed_at = null"), "terminal transition must release the claim");
        assertTrue(source.contains("next_retry_at = null"), "terminal/manual transition must clear retry time");
        assertTrue(source.contains("last_error = null"), "successful/manual transition must clear old error");

        assertTrue(source.contains("int recordfailure("), "missing explicit failure transition");
        assertTrue(source.contains("next_retry_at = #{nextretryat}"),
                "failure transition must persist RETRY or DEAD retry time, including null");

        assertTrue(source.contains("int resetformanualretry("), "missing explicit manual retry transition");
        assertTrue(source.contains("attempts = 0"), "manual retry must reset attempt count");
    }
}
