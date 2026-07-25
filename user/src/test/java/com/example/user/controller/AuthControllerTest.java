package com.example.user.controller;

import com.example.common.advice.GlobalExceptionHandler;
import com.example.user.config.CustomRsaComponent;
import com.example.user.entity.User;
import com.example.user.service.AuthenticationService;
import com.example.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

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

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void userLoginAuthenticatesDecryptedPasswordAndReturnsToken() throws Exception {
        User user = user(101L, "buyer@example.com");
        when(customRsaComponent.decrypt("cipher")).thenReturn("raw-password");
        when(authenticationService.authenticate("buyer@example.com", "raw-password")).thenReturn(user);
        when(authenticationService.generateToken(user)).thenReturn("user-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("buyer@example.com", "cipher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.email").value("buyer@example.com"))
                .andExpect(jsonPath("$.data.token").value("user-token"));

        verify(authenticationService).authenticate("buyer@example.com", "raw-password");
    }

    @Test
    void invalidCredentialsAreReturnedAsUnauthorized() throws Exception {
        when(customRsaComponent.decrypt("cipher")).thenReturn("wrong-password");
        when(authenticationService.authenticate("buyer@example.com", "wrong-password"))
                .thenThrow(new com.example.common.exception.CustomUnauthorizedException("用户名或密码错误"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("buyer@example.com", "cipher")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminLoginAllowsAdminRole() throws Exception {
        User admin = user(201L, "admin@example.com");
        when(customRsaComponent.decrypt("cipher")).thenReturn("raw-password");
        when(authenticationService.authenticate("admin@example.com", "raw-password")).thenReturn(admin);
        when(userService.getRoleCodesByUserId(201L)).thenReturn(List.of("ADMIN"));
        when(authenticationService.generateToken(admin)).thenReturn("admin-token");

        mockMvc.perform(post("/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@example.com", "cipher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("admin-token"));
    }

    @Test
    void adminLoginRejectsNormalUserRole() throws Exception {
        User user = user(301L, "buyer@example.com");
        when(customRsaComponent.decrypt("cipher")).thenReturn("raw-password");
        when(authenticationService.authenticate("buyer@example.com", "raw-password")).thenReturn(user);
        when(userService.getRoleCodesByUserId(301L)).thenReturn(List.of("USER"));

        mockMvc.perform(post("/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("buyer@example.com", "cipher")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("权限不足，您不具备访问后台管理系统的权限"));

        verify(authenticationService, never()).generateToken(user);
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private String loginBody(String email, String encryptedPassword) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + encryptedPassword + "\"}";
    }
}
