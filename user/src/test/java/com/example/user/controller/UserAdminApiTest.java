package com.example.user.controller;

import com.example.common.advice.GlobalExceptionHandler;
import com.example.common.core.UserContext;
import com.example.common.interceptor.LoginInterceptor;
import com.example.common.response.PageResponse;
import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import com.example.common.security.TokenBlacklistChecker;
import com.example.user.controller.admin.UserAdminController;
import com.example.user.response.UserAdminResponse;
import com.example.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAdminApiTest {
    private static final String SECRET = "user-admin-api-test-secret-long-enough";

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        LoginInterceptor interceptor = new LoginInterceptor();
        ReflectionTestUtils.setField(interceptor, "secretKey", SECRET);
        TokenBlacklistChecker checker = mock(TokenBlacklistChecker.class);
        when(checker.isBlacklisted(anyString())).thenReturn(false);
        ReflectionTestUtils.setField(interceptor, "tokenBlacklistChecker", checker);
        UserAdminController controller = new UserAdminController(userService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void clearContext() {
        UserContext.remove();
    }

    @Test
    void noTokenIs401() throws Exception {
        mockMvc.perform(get("/admin/user/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void normalUserTokenIs403() throws Exception {
        mockMvc.perform(get("/admin/user/list")
                        .header("Authorization", "Bearer " + token(List.of("USER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void superAdminCanListAndResponseHasNoPassword() throws Exception {
        UserAdminResponse row = new UserAdminResponse();
        row.setId(178L);
        row.setEmail("admin@example.com");
        row.setRoleCodes(List.of("SUPER_ADMIN"));
        PageResponse<UserAdminResponse> page = new PageResponse<>();
        page.setTotal(1L);
        page.setList(List.of(row));
        when(userService.adminList(any())).thenReturn(page);

        mockMvc.perform(get("/admin/user/list")
                        .header("Authorization", "Bearer " + token(List.of("SUPER_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].id").value("178"))
                .andExpect(jsonPath("$.data.list[0].email").value("admin@example.com"))
                .andExpect(jsonPath("$.data.list[0].password").doesNotExist());
    }

    @Test
    void missingEnabledIs422AndDoesNotCallService() throws Exception {
        mockMvc.perform(put("/admin/user/256/enabled")
                        .header("Authorization", "Bearer " + token(List.of("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));

        verifyNoInteractions(userService);
    }

    @Test
    void emptyRoleIdsIs422AndDoesNotCallService() throws Exception {
        mockMvc.perform(put("/admin/user/256/roles")
                        .header("Authorization", "Bearer " + token(List.of("SUPER_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));

        verifyNoInteractions(userService);
    }

    @Test
    void adminCannotReplaceUserRoles() throws Exception {
        mockMvc.perform(put("/admin/user/256/roles")
                        .header("Authorization", "Bearer " + token(List.of("ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[2,3]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verifyNoInteractions(userService);
    }

    @Test
    void adminCanReadDetailAndUpdateEnabled() throws Exception {
        UserAdminResponse detail = new UserAdminResponse();
        detail.setId(256L);
        when(userService.adminDetail(256L)).thenReturn(detail);
        when(userService.updateEnabled(256L, false)).thenReturn(true);
        String authorization = "Bearer " + token(List.of("ADMIN"));

        mockMvc.perform(get("/admin/user/256").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("256"));
        mockMvc.perform(put("/admin/user/256/enabled")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        verify(userService).adminDetail(256L);
        verify(userService).updateEnabled(256L, false);
    }

    @Test
    void superAdminCanReplaceUserRoles() throws Exception {
        when(userService.replaceRoles(256L, List.of(2L, 3L))).thenReturn(true);

        mockMvc.perform(put("/admin/user/256/roles")
                        .header("Authorization", "Bearer " + token(List.of("SUPER_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[2,3]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(userService).replaceRoles(256L, List.of(2L, 3L));
    }

    private String token(List<String> roles) {
        return JwtUtil.sign(Map.of(JwtClaims.ID, 178L, JwtClaims.ROLES, roles), SECRET,
                new Date(System.currentTimeMillis() - 1000),
                new Date(System.currentTimeMillis() + 60_000));
    }
}
