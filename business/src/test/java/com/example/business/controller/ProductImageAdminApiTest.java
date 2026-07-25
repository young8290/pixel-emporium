package com.example.business.controller;

import com.example.business.controller.admin.ProductImageAdminController;
import com.example.business.entity.ProductImage;
import com.example.business.response.ProductImageResponse;
import com.example.business.service.ProductImageService;
import com.example.business.service.ProductImageStyleService;
import com.example.common.advice.GlobalExceptionHandler;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import com.example.common.security.TokenBlacklistChecker;
import com.example.common.response.R;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.common.interceptor.LoginInterceptor;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductImageAdminApiTest {
    private static final String SECRET = "product-image-test-secret";

    @Mock
    private ProductImageService imageService;
    @Mock
    private ProductImageStyleService styleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LoginInterceptor interceptor = new LoginInterceptor();
        ReflectionTestUtils.setField(interceptor, "secretKey", SECRET);
        TokenBlacklistChecker checker = mock(TokenBlacklistChecker.class);
        lenient().when(checker.isBlacklisted(anyString())).thenReturn(false);
        ReflectionTestUtils.setField(interceptor, "tokenBlacklistChecker", checker);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductImageAdminController(imageService, styleService))
                .addInterceptors(interceptor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void statusEndpointReturnsPendingStateForAdmin() throws Exception {
        ProductImageResponse response = new ProductImageResponse();
        response.setId(9L);
        response.setStyleStatus(ProductImage.StyleStatus.PENDING);
        when(imageService.status(9L)).thenReturn(response);

        mockMvc.perform(get("/admin/product-image/9/style-status")
                        .header("Authorization", bearerToken("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.id", is("9")))
                .andExpect(jsonPath("$.data.styleStatus", is("PENDING")));
    }

    @Test
    void nonAdminCannotTriggerStylize() throws Exception {
        mockMvc.perform(post("/admin/product-image/9/stylize")
                        .header("Authorization", bearerToken("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"presetKey\":\"stardew-soft\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is(403)));
    }

    @Test
    void controllerRequiresAdminRoleAnnotation() {
        CustomRequireRole annotation = ProductImageAdminController.class.getAnnotation(CustomRequireRole.class);
        assertTrue(annotation != null);
        assertArrayEquals(new String[]{"ADMIN", "SUPER_ADMIN"}, annotation.value());
    }

    private static String bearerToken(String role) {
        String token = JwtUtil.sign(Map.of(
                        JwtClaims.ID, 17L,
                        JwtClaims.ROLES, List.of(role)), SECRET,
                Date.from(Instant.now().minusSeconds(1)),
                Date.from(Instant.now().plusSeconds(600)));
        return "Bearer " + token;
    }
}
