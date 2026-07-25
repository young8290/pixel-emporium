package com.example.user.controller;

import com.example.common.advice.GlobalExceptionHandler;
import com.example.common.core.UserContext;
import com.example.common.interceptor.LoginInterceptor;
import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import com.example.common.security.TokenBlacklistChecker;
import com.example.user.controller.admin.MailAdminController;
import com.example.user.enums.MailTemplateKey;
import com.example.user.request.MailConfigUpdateRequest;
import com.example.user.response.MailConfigResponse;
import com.example.user.service.MailConfigService;
import com.example.user.service.MailDeliveryService;
import com.example.user.service.MailTemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.http.MediaType;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MailAdminControllerTest {
    private static final String SECRET = "mail-admin-api-test-secret-long-enough";

    private MailConfigService config;
    private MailDeliveryService delivery;
    private MailTemplateService templates;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        config = mock(MailConfigService.class);
        delivery = mock(MailDeliveryService.class);
        templates = mock(MailTemplateService.class);
        LoginInterceptor interceptor = new LoginInterceptor();
        ReflectionTestUtils.setField(interceptor, "secretKey", SECRET);
        TokenBlacklistChecker checker = mock(TokenBlacklistChecker.class);
        when(checker.isBlacklisted(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        ReflectionTestUtils.setField(interceptor, "tokenBlacklistChecker", checker);
        mockMvc = MockMvcBuilders.standaloneSetup(new MailAdminController(config, delivery, templates))
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearContext() {
        UserContext.remove();
    }

    @Test
    void mailAdministrationRequiresSuperAdminAndNeverReturnsCiphertext() throws Exception {
        mockMvc.perform(get("/admin/mail/config"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/mail/config").header("Authorization", token(List.of("ADMIN"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(config);

        MailConfigResponse response = new MailConfigResponse();
        response.setVersion(5);
        response.setPasswordConfigured(true);
        when(config.get()).thenReturn(response);
        mockMvc.perform(get("/admin/mail/config").header("Authorization", token(List.of("SUPER_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(5))
                .andExpect(jsonPath("$.data.passwordConfigured").value(true))
                .andExpect(jsonPath("$.data.passwordCiphertext").doesNotExist());
    }

    @Test
    void previewIsReturnedAsUtf8Html() throws Exception {
        when(templates.preview(com.example.user.enums.MailTemplateKey.VERIFICATION_CODE)).thenReturn("<p>验证码</p>");

        mockMvc.perform(get("/admin/mail/preview").param("templateKey", "VERIFICATION_CODE")
                        .header("Authorization", token(List.of("SUPER_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"));
    }

    @Test
    void invalidPreviewTemplateIsRejectedAsBadRequest() throws Exception {
        mockMvc.perform(get("/admin/mail/preview").param("templateKey", "ARBITRARY_HTML")
                        .header("Authorization", token(List.of("SUPER_ADMIN"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(templates);
    }

    @Test
    void superAdminCanUpdateConfigurationAndResponseRemainsSafe() throws Exception {
        MailConfigResponse response = new MailConfigResponse();
        response.setVersion(6);
        response.setPasswordConfigured(true);
        when(config.get()).thenReturn(response);

        mockMvc.perform(put("/admin/mail/config")
                        .header("Authorization", token(List.of("SUPER_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 5,
                                  "enabled": true,
                                  "host": "smtp.example.com",
                                  "port": 587,
                                  "securityMode": "STARTTLS",
                                  "username": "shop@example.com",
                                  "password": "smtp-secret",
                                  "fromAddress": "shop@example.com",
                                  "fromName": "Pixel Shop",
                                  "replyTo": "support@example.com",
                                  "connectionTimeoutMs": 5000,
                                  "readTimeoutMs": 10000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(6))
                .andExpect(jsonPath("$.data.passwordCiphertext").doesNotExist());

        verify(config).update(any(MailConfigUpdateRequest.class));
    }

    @Test
    void connectionTestDoesNotQueueMailAndTestSendDoes() throws Exception {
        JavaMailSenderImpl sender = mock(JavaMailSenderImpl.class);
        when(config.sender()).thenReturn(sender);

        mockMvc.perform(post("/admin/mail/test-connection")
                        .header("Authorization", token(List.of("SUPER_ADMIN"))))
                .andExpect(status().isOk());
        verify(sender).testConnection();
        verifyNoInteractions(delivery);

        mockMvc.perform(post("/admin/mail/test-send")
                        .header("Authorization", token(List.of("SUPER_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipient\":\" buyer@example.com \",\"templateKey\":\"ORDER_CREATED\"}"))
                .andExpect(status().isOk());
        verify(delivery).queueTest("buyer@example.com", MailTemplateKey.ORDER_CREATED);
    }

    @Test
    void retryEndpointDelegatesStatusValidationToDeliveryService() throws Exception {
        mockMvc.perform(post("/admin/mail/delivery/77/retry")
                        .header("Authorization", token(List.of("SUPER_ADMIN"))))
                .andExpect(status().isOk());

        verify(delivery).retry(77L);
    }

    private String token(List<String> roles) {
        return JwtUtil.sign(Map.of(JwtClaims.ID, 178L, JwtClaims.ROLES, roles), SECRET,
                new Date(System.currentTimeMillis() - 1_000), new Date(System.currentTimeMillis() + 60_000));
    }
}
