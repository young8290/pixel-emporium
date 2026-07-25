package com.example.user.controller;

import com.example.user.controller.AuthController;
import com.example.user.request.EmailCodeRequest;
import com.example.user.response.EmailCodeResponse;
import com.example.user.service.EmailVerificationService;
import com.example.user.util.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EmailVerificationControllerTest {
    @Test
    void successfulRequestUsesTheGenericRequiredMessage() {
        AuthController controller = new AuthController();
        EmailVerificationService service = mock(EmailVerificationService.class);
        ClientIpResolver resolver = mock(ClientIpResolver.class);
        ReflectionTestUtils.setField(controller, "emailVerificationService", service);
        ReflectionTestUtils.setField(controller, "clientIpResolver", resolver);
        when(resolver.resolve(any())).thenReturn("127.0.0.1");
        when(service.sendRegistrationCode(anyString(), anyString())).thenReturn(new EmailCodeResponse(60, 600));
        EmailCodeRequest request = new EmailCodeRequest(); request.setEmail("owner@example.com");
        assertEquals("若邮箱可用于注册，验证码已发送", controller.sendEmailCode(request, new MockHttpServletRequest()).getMsg());
    }
}
