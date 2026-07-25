package com.example.user;

import com.example.user.config.CustomRsaComponent;
import com.example.user.entity.User;
import com.example.user.service.UserService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("mysql-integration")
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserLogicDeleteUniqueTest {

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private CustomRsaComponent customRsaComponent;

    @Test
    void deletedUserEmailCanBeReused() {
        String token = UUID.randomUUID().toString().replace("-", "");
        String email = "logic-delete-" + token + "@example.com";
        String mobileSuffix = String.format(
                "%018d",
                Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000_000_000_000L));

        User original = user(email, "logic-delete-original-" + token, "1" + mobileSuffix);
        assertTrue(userService.save(original));
        assertTrue(userService.removeById(original.getId()));

        assertNull(userService.getById(original.getId()));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "select deleted from `user` where id = ?",
                Long.class,
                original.getId()));

        User replacement = user(email, "logic-delete-replacement-" + token, "2" + mobileSuffix);
        assertTrue(userService.save(replacement));
    }

    private User user(String email, String name, String mobile) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("test-password");
        user.setName(name);
        user.setMobile(mobile);
        user.setEnabled(true);
        return user;
    }
}
