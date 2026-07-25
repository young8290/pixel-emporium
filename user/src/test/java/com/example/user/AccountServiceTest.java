package com.example.user;

import com.example.common.core.UserContext;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomValidationException;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.user.request.ProfileUpdateRequest;
import com.example.user.response.ProfileResponse;
import com.example.user.security.PasswordPolicy;
import com.example.user.service.TokenBlacklistService;
import com.example.user.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private UserMapper userMapper;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(userMapper, tokenBlacklistService, new PasswordPolicy());
        UserContext.set(42L);
    }

    @AfterEach
    void clearContext() {
        UserContext.remove();
    }

    @Test
    void profileReturnsOnlyCurrentUserPublicFields() {
        when(userMapper.selectById(42L)).thenReturn(user("buyer@example.com", "田园店主", null, null));

        ProfileResponse response = service.profile();

        assertEquals("buyer@example.com", response.getEmail());
        assertEquals("田园店主", response.getName());
        assertNull(response.getMobile());
    }

    @Test
    void updateProfileUsesCurrentUserAndNormalizesOptionalFields() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName(" 新昵称 ");
        request.setMobile("");
        request.setAvatar(" https://cdn.example.com/avatar.png ");
        when(userMapper.updateProfileById(42L, "新昵称", null, "https://cdn.example.com/avatar.png"))
                .thenReturn(1);
        when(userMapper.selectById(42L)).thenReturn(
                user("buyer@example.com", "新昵称", null, "https://cdn.example.com/avatar.png"));

        ProfileResponse response = service.updateProfile(request);

        assertEquals("新昵称", response.getName());
        verify(userMapper).updateProfileById(42L, "新昵称", null, "https://cdn.example.com/avatar.png");
    }

    @Test
    void duplicateNameIsRejectedBeforeUpdate() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName("已存在");
        when(userMapper.countActiveByNameExcludingId("已存在", 42L)).thenReturn(1L);

        assertThrows(CustomConflictException.class, () -> service.updateProfile(request));

        verify(userMapper, never()).updateProfileById(42L, "已存在", null, null);
    }

    @Test
    void changePasswordVerifiesCurrentPasswordAndRevokesExistingSessions() {
        User user = user("buyer@example.com", "店主", null, null);
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("Old password 2026"));
        when(userMapper.selectByIdForUpdate(42L)).thenReturn(user);
        when(userMapper.updatePasswordAndIncrementTokenVersion(
                org.mockito.ArgumentMatchers.eq(42L), argThat(hash ->
                        cn.hutool.crypto.digest.BCrypt.checkpw("New password 2026!", hash))))
                .thenReturn(1);
        when(userMapper.selectTokenVersionById(42L)).thenReturn(3L);
        when(tokenBlacklistService.storeTokenVersion(42L, 3L)).thenReturn(3L);

        service.changePassword("Old password 2026", "New password 2026!");

        verify(tokenBlacklistService).storeTokenVersion(42L, 3L);
    }

    @Test
    void wrongCurrentPasswordDoesNotRotateSession() {
        User user = user("buyer@example.com", "店主", null, null);
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("Old password 2026"));
        when(userMapper.selectByIdForUpdate(42L)).thenReturn(user);

        assertThrows(CustomValidationException.class,
                () -> service.changePassword("wrong password", "New password 2026!"));

        verify(userMapper, never()).selectTokenVersionById(42L);
        verify(tokenBlacklistService, never()).storeTokenVersion(42L, 1L);
    }

    private User user(String email, String name, String mobile, String avatar) {
        User user = new User();
        user.setId(42L);
        user.setEmail(email);
        user.setName(name);
        user.setMobile(mobile);
        user.setAvatar(avatar);
        user.setPassword("must-not-leak");
        return user;
    }
}
