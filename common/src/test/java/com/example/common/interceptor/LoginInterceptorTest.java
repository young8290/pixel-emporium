package com.example.common.interceptor;

import com.example.common.annotation.CustomRequireRole;
import com.example.common.core.UserContext;
import com.example.common.exception.CustomForbiddenException;
import com.example.common.exception.CustomUnauthorizedException;
import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import com.example.common.security.TokenBlacklistChecker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginInterceptorTest {
    private static final String SECRET = "interceptor-test-secret-which-is-long-enough";

    @AfterEach
    void clearContext() {
        UserContext.remove();
    }

    @Test
    void blacklistedTokenIsRejectedBeforeContextIsSet() throws Exception {
        TokenBlacklistChecker checker = mock(TokenBlacklistChecker.class);
        when(checker.isBlacklisted(anyString())).thenReturn(true);
        LoginInterceptor interceptor = interceptor(checker);
        String token = token(Map.of(JwtClaims.ID, 41L));

        HttpServletRequest request = requestWithToken(token);
        assertThrows(CustomUnauthorizedException.class,
                () -> interceptor.preHandle(request, mock(HttpServletResponse.class), plainHandler()));
        assertNull(UserContext.get());
        verify(checker).isBlacklisted(token);
    }

    @Test
    void missingBlacklistCheckerFailsClosed() throws Exception {
        LoginInterceptor interceptor = interceptor(null);
        String token = token(Map.of(JwtClaims.ID, 42L));

        assertThrows(IllegalStateException.class,
                () -> interceptor.preHandle(requestWithToken(token), mock(HttpServletResponse.class), plainHandler()));
        assertNull(UserContext.get());
    }

    @Test
    void rejectedRoleDoesNotLeaveUserContext() throws Exception {
        LoginInterceptor interceptor = interceptor(activeChecker());
        String token = token(Map.of(JwtClaims.ID, 43L, JwtClaims.ROLES, List.of("USER")));

        assertThrows(CustomForbiddenException.class,
                () -> interceptor.preHandle(requestWithToken(token), mock(HttpServletResponse.class), restrictedHandler()));
        assertNull(UserContext.get());
    }

    @Test
    void afterCompletionRemovesContext() throws Exception {
        LoginInterceptor interceptor = interceptor(activeChecker());
        String token = token(Map.of(JwtClaims.ID, 44L));
        HttpServletRequest request = requestWithToken(token);
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertTrue(interceptor.preHandle(request, response, plainHandler()));
        assertEquals(44L, UserContext.get());
        interceptor.afterCompletion(request, response, plainHandler(), null);
        assertNull(UserContext.get());
    }

    @Test
    void invalidTokenIsUnauthorizedAndClearsStaleContext() {
        UserContext.set(999L);
        LoginInterceptor interceptor = interceptor(null);

        assertThrows(CustomUnauthorizedException.class,
                () -> interceptor.preHandle(requestWithToken("not-a-token"), mock(HttpServletResponse.class), plainHandler()));
        assertNull(UserContext.get());
    }

    private LoginInterceptor interceptor(TokenBlacklistChecker checker) {
        LoginInterceptor interceptor = new LoginInterceptor();
        ReflectionTestUtils.setField(interceptor, "secretKey", SECRET);
        if (checker != null) {
            ReflectionTestUtils.setField(interceptor, "tokenBlacklistChecker", checker);
        }
        return interceptor;
    }

    private TokenBlacklistChecker activeChecker() {
        TokenBlacklistChecker checker = mock(TokenBlacklistChecker.class);
        when(checker.isBlacklisted(anyString())).thenReturn(false);
        return checker;
    }

    private String token(Map<String, Object> claims) {
        return JwtUtil.sign(claims, SECRET,
                new Date(System.currentTimeMillis() - 1000),
                new Date(System.currentTimeMillis() + 60_000));
    }

    private HttpServletRequest requestWithToken(String token) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/protected");
        return request;
    }

    private Object plainHandler() {
        return new Object();
    }

    private HandlerMethod restrictedHandler() throws NoSuchMethodException {
        Method method = FixtureController.class.getDeclaredMethod("adminOnly");
        return new HandlerMethod(new FixtureController(), method);
    }

    static class FixtureController {
        @CustomRequireRole("ADMIN")
        public void adminOnly() {
        }
    }
}
