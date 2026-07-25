package com.example.user;

import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomUnauthorizedException;
import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import com.example.user.entity.User;
import com.example.user.mapper.UserMapper;
import com.example.user.security.PasswordPolicy;
import com.example.user.service.AuthenticationService;
import com.example.user.service.LoginAttemptService;
import com.example.user.service.TokenBlacklistService;
import com.example.user.service.UserService;
import com.example.user.service.EmailVerificationService;
import com.example.user.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthEnhanceTest {

    private static final String SECRET = "test-secret";

    @Mock
    private UserService userService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Spy
    private PasswordPolicy passwordPolicy = new PasswordPolicy();

    @InjectMocks
    private AuthenticationServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "secretKey", SECRET);
        ReflectionTestUtils.setField(service, "expiresIn", 86_400_000L);
        lenient().when(emailVerificationService.normalizeEmail(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0, String.class).trim().toLowerCase());
    }

    @Test
    void generateTokenIncludesSessionVersionAndLegacyTokenIsRejected() {
        User user = user(42L, "u@example.com");
        user.setTokenVersion(3L);
        when(userService.getRoleCodesByUserId(42L)).thenReturn(List.of("USER"));
        when(userService.getPermissionCodesByUserId(42L)).thenReturn(List.of("catalog:read"));
        when(tokenBlacklistService.storeTokenVersion(42L, 3L)).thenReturn(3L);

        String token = service.generateToken(user);

        assertNotNull(JwtUtil.getJti(token));
        assertEquals(3L, ((Number) JwtUtil.getClaim(token, JwtClaims.TOKEN_VERSION)).longValue());
        assertTrue(service.validateToken(token));
        verify(tokenBlacklistService).storeTokenVersion(42L, 3L);

        String legacy = JwtUtil.sign(Map.of(JwtClaims.ID, 42L), SECRET,
                new Date(System.currentTimeMillis() - 1000),
                new Date(System.currentTimeMillis() + 60_000));
        assertNull(JwtUtil.getJti(legacy));
        assertFalse(service.validateToken(legacy));
    }

    @Test
    void generateTokenRejectsStaleAdminSnapshotAfterVersionAdvanced() {
        User staleAdmin = user(42L, "admin@example.com");
        staleAdmin.setTokenVersion(0L);
        when(userService.getRoleCodesByUserId(42L)).thenReturn(List.of("ADMIN"));
        when(userService.getPermissionCodesByUserId(42L)).thenReturn(List.of("admin:write"));
        when(tokenBlacklistService.storeTokenVersion(42L, 0L)).thenReturn(1L);

        assertThrows(CustomUnauthorizedException.class,
                () -> service.generateToken(staleAdmin));

        verify(tokenBlacklistService).storeTokenVersion(42L, 0L);
    }

    @Test
    void authenticateNormalizesEmailAndClearsFailureCounterOnSuccess() {
        User user = user(42L, "u@example.com");
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("secret"));
        when(userService.getOne(any())).thenReturn(user);

        User authenticated = service.authenticate("  U@EXAMPLE.COM ", "secret");

        assertSame(user, authenticated);
        verify(loginAttemptService).assertAllowed("u@example.com");
        verify(loginAttemptService).onSuccess("u@example.com");
        verify(loginAttemptService, never()).onFailure(anyString());
    }

    @Test
    void disabledUserCannotAuthenticateEvenWithCorrectPassword() {
        User disabled = user(42L, "u@example.com");
        disabled.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("secret"));
        disabled.setEnabled(false);
        when(userService.getOne(any())).thenReturn(disabled);

        CustomUnauthorizedException error = assertThrows(CustomUnauthorizedException.class,
                () -> service.authenticate("u@example.com", "secret"));

        assertEquals("账号已停用", error.getMessage());
        verify(loginAttemptService).onSuccess("u@example.com");
        verify(loginAttemptService, never()).onFailure(anyString());
    }

    @Test
    void authenticateUsesGenericFailureAndCountsUnknownUser() {
        when(userService.getOne(any())).thenReturn(null);

        CustomUnauthorizedException error = assertThrows(CustomUnauthorizedException.class,
                () -> service.authenticate("NoUser@Example.com", "bad"));

        assertEquals("邮箱或密码错误", error.getMessage());
        verify(loginAttemptService).assertAllowed("nouser@example.com");
        verify(loginAttemptService).onFailure("nouser@example.com");
    }

    @Test
    void authenticateUsesGenericFailureAndCountsWrongPassword() {
        User user = user(42L, "u@example.com");
        user.setPassword(cn.hutool.crypto.digest.BCrypt.hashpw("secret"));
        when(userService.getOne(any())).thenReturn(user);

        CustomUnauthorizedException error = assertThrows(CustomUnauthorizedException.class,
                () -> service.authenticate("u@example.com", "wrong"));

        assertEquals("邮箱或密码错误", error.getMessage());
        verify(loginAttemptService).onFailure("u@example.com");
        verify(loginAttemptService, never()).onSuccess(anyString());
    }

    @Test
    void registerHashesPasswordEnablesUserAndAssignsDefaultRole() {
        when(userService.getOne(any())).thenReturn(null);
        when(userService.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(99L);
            return true;
        });
        when(userMapper.insertDefaultUserRole(anyLong(), anyLong())).thenReturn(1);

        when(emailVerificationService.normalizeEmail("  NEW@Example.COM ")).thenReturn("new@example.com");
        assertTrue(service.register("  NEW@Example.COM ", "123456", "Valid secret 2026", "New User", "13800138000"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("new@example.com", saved.getEmail());
        assertEquals("New User", saved.getName());
        assertEquals("13800138000", saved.getMobile());
        assertTrue(saved.getEnabled());
        assertNotEquals("Valid secret 2026", saved.getPassword());
        assertTrue(cn.hutool.crypto.digest.BCrypt.checkpw("Valid secret 2026", saved.getPassword()));
        verify(userMapper).insertDefaultUserRole(eq(99L), anyLong());
    }

    @Test
    void registerRejectsDuplicateEmailWithConflict() {
        when(userService.getOne(any())).thenReturn(user(7L, "existing@example.com"));

        assertThrows(CustomConflictException.class,
                () -> service.register("existing@example.com", "123456", "Valid secret 2026", "Existing", null));
        verify(userService, never()).save(any(User.class));
        verify(userMapper, never()).insertDefaultUserRole(anyLong(), anyLong());
    }

    @Test
    void registerRejectsDuplicateNameWithConflict() {
        when(userService.getOne(any())).thenReturn(null).thenReturn(user(7L, "existing@example.com"));

        assertThrows(CustomConflictException.class,
                () -> service.register("new@example.com", "123456", "Valid secret 2026", "Existing", null));
        verify(userService, never()).save(any(User.class));
    }

    @Test
    void registerRejectsDuplicateMobileWithConflict() {
        when(userService.getOne(any())).thenReturn(null).thenReturn(null)
                .thenReturn(user(7L, "existing@example.com"));

        assertThrows(CustomConflictException.class,
                () -> service.register("new@example.com", "123456", "Valid secret 2026", "New User", "13800138000"));
        verify(userService, never()).save(any(User.class));
    }

    @Test
    void registerConvertsDatabaseDuplicateRaceToConflict() {
        when(userService.getOne(any())).thenReturn(null);
        when(userService.save(any(User.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertThrows(CustomConflictException.class,
                () -> service.register("new@example.com", "123456", "Valid secret 2026", "New User", null));
        verify(userMapper, never()).insertDefaultUserRole(anyLong(), anyLong());
    }

    @Test
    void blockedLoginIsRejectedBeforeUserLookup() {
        doThrow(new CustomUnauthorizedException("登录失败次数过多，请稍后重试"))
                .when(loginAttemptService).assertAllowed("u@example.com");

        assertThrows(CustomUnauthorizedException.class,
                () -> service.authenticate("u@example.com", "secret"));
        verify(userService, never()).getOne(any());
        verify(loginAttemptService, never()).onFailure(anyString());
    }

    @Test
    void logoutDelegatesToBlacklistService() {
        service.logout("token");
        verify(tokenBlacklistService).blacklist("token");
    }

    @Test
    void refreshBlacklistsOldTokenAndIssuesReplacement() {
        User user = user(42L, "u@example.com");
        when(userService.getById(42L)).thenReturn(user);
        when(userService.getRoleCodesByUserId(42L)).thenReturn(List.of("USER"));
        when(userService.getPermissionCodesByUserId(42L)).thenReturn(List.of());
        when(tokenBlacklistService.blacklistIfNotBlacklisted(anyString())).thenReturn(true);

        String oldToken = service.generateToken(user);
        String refreshed = service.refreshToken(oldToken);

        assertNotEquals(oldToken, refreshed);
        assertTrue(service.validateToken(refreshed));
        assertNotEquals(JwtUtil.getJti(oldToken), JwtUtil.getJti(refreshed));
        verify(tokenBlacklistService).blacklistIfNotBlacklisted(oldToken);
    }

    @Test
    void refreshRejectsBlacklistedToken() {
        String revoked = JwtUtil.sign(Map.of(JwtClaims.ID, 42L, JwtClaims.JTI, "revoked-jti",
                        JwtClaims.TOKEN_VERSION, 0L), SECRET,
                new Date(System.currentTimeMillis() - 1000),
                new Date(System.currentTimeMillis() + 60_000));
        when(tokenBlacklistService.isBlacklisted(revoked)).thenReturn(true);

        assertThrows(CustomUnauthorizedException.class, () -> service.refreshToken(revoked));
        verify(userService, never()).getById(anyLong());
    }

    @Test
    void refreshRejectsInvalidToken() {
        assertThrows(CustomUnauthorizedException.class, () -> service.refreshToken("not-a-jwt"));
        verifyNoInteractions(userService, tokenBlacklistService);
    }

    @Test
    void refreshRejectsTokenWhenUserDoesNotExist() {
        when(userService.getById(42L)).thenReturn(null);
        String token = JwtUtil.sign(Map.of(JwtClaims.ID, 42L, JwtClaims.JTI, "old-jti",
                        JwtClaims.TOKEN_VERSION, 0L), SECRET,
                new Date(System.currentTimeMillis() - 1000),
                new Date(System.currentTimeMillis() + 60_000));

        assertThrows(CustomUnauthorizedException.class, () -> service.refreshToken(token));
        verify(tokenBlacklistService, never()).blacklist(anyString());
    }

    @Test
    void refreshRejectsDisabledUserBeforeBlacklistingOldToken() {
        User disabled = user(42L, "u@example.com");
        disabled.setEnabled(false);
        when(userService.getById(42L)).thenReturn(disabled);
        String token = JwtUtil.sign(Map.of(JwtClaims.ID, 42L, JwtClaims.JTI, "old-jti",
                        JwtClaims.TOKEN_VERSION, 0L), SECRET,
                new Date(System.currentTimeMillis() - 1000),
                new Date(System.currentTimeMillis() + 60_000));

        assertThrows(CustomUnauthorizedException.class, () -> service.refreshToken(token));
        verify(tokenBlacklistService, never()).blacklistIfNotBlacklisted(anyString());
    }

    @Test
    void refreshRejectsTokenVersionMismatchWithoutRotatingJti() {
        User current = user(42L, "u@example.com");
        current.setTokenVersion(1L);
        when(userService.getById(42L)).thenReturn(current);
        String staleToken = JwtUtil.sign(Map.of(
                        JwtClaims.ID, 42L,
                        JwtClaims.JTI, "stale-jti",
                        JwtClaims.TOKEN_VERSION, 0L), SECRET,
                new Date(System.currentTimeMillis() - 1_000),
                new Date(System.currentTimeMillis() + 60_000));

        assertThrows(CustomUnauthorizedException.class,
                () -> service.refreshToken(staleToken));

        verify(tokenBlacklistService, never()).blacklistIfNotBlacklisted(anyString());
        verify(tokenBlacklistService, never()).storeTokenVersion(anyLong(), anyLong());
    }

    @Test
    void generateTokenRejectsDisabledUser() {
        User disabled = user(42L, "u@example.com");
        disabled.setEnabled(false);

        assertThrows(CustomUnauthorizedException.class, () -> service.generateToken(disabled));
        verifyNoInteractions(userService);
    }

    @Test
    void legacyTokenCannotRefresh() {
        String legacy = JwtUtil.sign(Map.of(JwtClaims.ID, 42L), SECRET,
                new Date(System.currentTimeMillis() - 1000),
                new Date(System.currentTimeMillis() + 60_000));

        assertThrows(CustomUnauthorizedException.class, () -> service.refreshToken(legacy));
        verifyNoInteractions(userService);
    }

    private static User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("User");
        user.setEnabled(true);
        return user;
    }
}
