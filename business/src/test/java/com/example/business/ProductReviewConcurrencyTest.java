package com.example.business;

import com.example.business.mapper.ProductMapper;
import com.example.business.mapper.ProductReviewMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductReviewConcurrencyTest {
    @Test
    void productLockAndOwnerScopedReviewMutationsCloseRaceWindows() throws Exception {
        assertTrue(select(ProductMapper.class.getMethod("selectByIdForUpdate", Long.class))
                .contains("for update"));

        String update = update(ProductReviewMapper.class.getMethod(
                "updatePublishedByOwner", Long.class, Long.class, Integer.class, String.class));
        assertTrue(update.contains("user_id = #{userid}"));
        assertTrue(update.contains("status = 'published'"));
        assertTrue(update.contains("deleted = 0"));

        String delete = update(ProductReviewMapper.class.getMethod(
                "softDeleteByOwner", Long.class, Long.class));
        assertTrue(delete.contains("set deleted = id"));
        assertTrue(delete.contains("user_id = #{userid}"));
        assertTrue(delete.contains("deleted = 0"));
    }

    private String select(Method method) {
        return normalize(String.join(" ", method.getAnnotation(Select.class).value()));
    }

    private String update(Method method) {
        return normalize(String.join(" ", method.getAnnotation(Update.class).value()));
    }

    private String normalize(String sql) {
        return sql.toLowerCase().replaceAll("\\s+", " ");
    }
}
