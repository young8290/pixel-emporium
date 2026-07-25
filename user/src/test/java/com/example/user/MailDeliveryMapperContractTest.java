package com.example.user;

import com.example.user.mapper.MailDeliveryMapper;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailDeliveryMapperContractTest {
    @Test
    void markSentSqlClearsTransientDeliveryState() {
        String sql = sqlFor("markSent", Long.class, LocalDateTime.class);

        assertContainsAll(sql, "status = 'SENT'", "sent_at =", "claimed_at = null",
                "next_retry_at = null", "last_error = null");
    }

    @Test
    void resetForManualRetrySqlClearsTransientDeliveryState() {
        String sql = sqlFor("resetForManualRetry", Long.class, String.class);

        assertContainsAll(sql, "status = 'PENDING'", "attempts = 0", "recipient =",
                "claimed_at = null", "next_retry_at = null", "last_error = null");
    }

    @Test
    void recordFailureSqlPersistsAConsistentFailureSnapshot() {
        String sql = sqlFor("recordFailure", Long.class, String.class, Integer.class,
                LocalDateTime.class, String.class);

        assertContainsAll(sql, "status =", "attempts =", "claimed_at = null",
                "next_retry_at =", "last_error =");
    }

    @Test
    void claimAndRecoverySqlUseApplicationClockParameters() {
        String claimSql = sqlFor("claim", Long.class, LocalDateTime.class);
        String recoverySql = sqlFor("recoverStaleClaims", LocalDateTime.class, LocalDateTime.class);

        assertContainsAll(claimSql, "claimed_at = #{now}", "next_retry_at <= #{now}");
        assertContainsAll(recoverySql, "next_retry_at = #{now}", "claimed_at < #{staleBefore}");
    }

    private String sqlFor(String methodName, Class<?>... parameterTypes) {
        Method method = Arrays.stream(MailDeliveryMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .filter(candidate -> Arrays.equals(candidate.getParameterTypes(), parameterTypes))
                .findFirst()
                .orElse(null);
        assertNotNull(method, "Missing semantic mapper method: " + methodName);
        Update update = method.getAnnotation(Update.class);
        assertNotNull(update, "Missing @Update SQL on " + methodName);
        return String.join(" ", update.value()).replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private void assertContainsAll(String sql, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(sql.contains(fragment.toLowerCase(Locale.ROOT)),
                    () -> "Expected SQL to contain '" + fragment + "' but was: " + sql);
        }
    }
}
