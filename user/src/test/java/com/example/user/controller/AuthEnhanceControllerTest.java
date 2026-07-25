package com.example.user.controller;

import com.example.common.advice.GlobalExceptionHandler;
import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import com.example.user.config.CustomRsaComponent;
import com.example.user.config.WebConfig;
import com.example.common.interceptor.LoginInterceptor;
import com.example.user.service.AuthenticationService;
import com.example.user.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.util.ServletRequestPathUtils;

import java.util.Date;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthEnhanceControllerTest {
    private static final String SECRET = "controller-test-secret-which-is-long-enough";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuthenticationService authenticationService;
    private UserService userService;
    private CustomRsaComponent customRsaComponent;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        userService = mock(UserService.class);
        customRsaComponent = mock(CustomRsaComponent.class);
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authenticationService", authenticationService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "customRsaComponent", customRsaComponent);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerDecryptsPasswordAndDelegates() throws Exception {
        when(customRsaComponent.decrypt("cipher-password")).thenReturn("raw-password");
        when(authenticationService.register("new@example.com", "123456", "raw-password", "New User", "13800138000"))
                .thenReturn(true);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"emailCode\":\"123456\",\"password\":\"cipher-password\","
                                + "\"name\":\"New User\",\"mobile\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        verify(authenticationService).register("new@example.com", "123456", "raw-password", "New User", "13800138000");
    }

    @Test
    void registerRejectsMalformedEncryptedPasswordAsValidationFailure() throws Exception {
        when(customRsaComponent.decrypt("invalid-cipher"))
                .thenThrow(new IllegalArgumentException("bad ciphertext"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"emailCode\":\"123456\",\"password\":\"invalid-cipher\","
                                + "\"name\":\"New User\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));

        verifyNoInteractions(authenticationService);
    }

    @Test
    void logoutRequiresStrictBearerTokenAndDelegates() throws Exception {
        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer logout-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        verify(authenticationService).logout("logout-token");
    }

    @Test
    void refreshReturnsEmailFromRefreshedJwt() throws Exception {
        String refreshed = JwtUtil.sign(
                Map.of(JwtClaims.EMAIL, "new@example.com"),
                SECRET,
                new Date(System.currentTimeMillis() - 1000),
                new Date(System.currentTimeMillis() + 60_000));
        when(authenticationService.refreshToken("old-token")).thenReturn(refreshed);

        String body = mockMvc.perform(post("/auth/refresh").header("Authorization", "Bearer old-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.email").value("new@example.com"))
                .andExpect(jsonPath("$.data.token").value(refreshed))
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(body).get("data");
        org.junit.jupiter.api.Assertions.assertEquals("new@example.com", data.get("email").asText());
    }

    @Test
    void logoutRejectsMissingOrMalformedBearerHeader() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/logout").header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(authenticationService);
    }

    @Test
    void refreshRejectsBearerTokenContainingWhitespace() throws Exception {
        mockMvc.perform(post("/auth/refresh").header("Authorization", "Bearer token with-space"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(authenticationService);
    }

    @Test
    void registerIsExcludedFromUserLoginInterceptor() {
        WebConfig webConfig = new WebConfig();
        ReflectionTestUtils.setField(webConfig, "loginInterceptor", new LoginInterceptor());
        ExposedInterceptorRegistry registry = new ExposedInterceptorRegistry();
        webConfig.addInterceptors(registry);

        MappedInterceptor mapped = (MappedInterceptor) registry.registered().get(0);
        MockHttpServletRequest register = requestWithCachedPath("/auth/register");
        MockHttpServletRequest login = requestWithCachedPath("/auth/login");
        org.junit.jupiter.api.Assertions.assertFalse(mapped.matches(register));
        org.junit.jupiter.api.Assertions.assertFalse(mapped.matches(login));
    }

    private MockHttpServletRequest requestWithCachedPath(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        ServletRequestPathUtils.parseAndCache(request);
        return request;
    }

    private static final class ExposedInterceptorRegistry extends InterceptorRegistry {
        java.util.List<Object> registered() {
            return getInterceptors();
        }
    }
}
