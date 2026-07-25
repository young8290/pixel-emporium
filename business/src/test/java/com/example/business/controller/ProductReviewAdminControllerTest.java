package com.example.business.controller;

import com.example.business.annotation.OperationLog;
import com.example.business.controller.admin.ProductReviewAdminController;
import com.example.business.request.ProductReviewAdminListRequest;
import com.example.business.response.ProductReviewAdminResponse;
import com.example.business.service.ProductReviewService;
import com.example.common.advice.GlobalExceptionHandler;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductReviewAdminControllerTest {
    @Mock
    private ProductReviewService reviewService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductReviewAdminController(reviewService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void controllerRequiresAdminRolesAndUsesContentFreeAuditAnnotations() throws Exception {
        CustomRequireRole role = ProductReviewAdminController.class
                .getAnnotation(CustomRequireRole.class);
        assertNotNull(role);
        assertEquals(Set.of("ADMIN", "SUPER_ADMIN"), Set.of(role.value()));

        Method hide = ProductReviewAdminController.class.getMethod(
                "hide", Long.class, com.example.business.request.ProductReviewModerateRequest.class);
        Method restore = ProductReviewAdminController.class.getMethod("restore", Long.class);
        OperationLog hideLog = hide.getAnnotation(OperationLog.class);
        OperationLog restoreLog = restore.getAnnotation(OperationLog.class);
        assertEquals("REVIEW_HIDE", hideLog.action());
        assertEquals("REVIEW_RESTORE", restoreLog.action());
        assertEquals("", hideLog.detail());
        assertEquals("#id", hideLog.targetIdSpEL());
    }

    @Test
    void listBindsFiltersAndSerializesSensitiveReviewIdsAsStrings() throws Exception {
        ProductReviewAdminResponse row = new ProductReviewAdminResponse();
        row.setId(9007199254740993L);
        row.setUserId(9007199254740994L);
        row.setOrderId(9007199254740995L);
        PageResponse<ProductReviewAdminResponse> page = new PageResponse<>();
        page.setTotal(1L);
        page.setList(List.of(row));
        when(reviewService.adminList(any())).thenReturn(page);

        mockMvc.perform(get("/admin/review/list")
                        .param("productId", "301")
                        .param("userId", "11")
                        .param("rating", "4")
                        .param("status", "HIDDEN")
                        .param("keyword", "像素")
                        .param("createdAtStart", "2026-07-01T00:00:00")
                        .param("createdAtEnd", "2026-07-31T23:59:59")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].id").value("9007199254740993"))
                .andExpect(jsonPath("$.data.list[0].userId").value("9007199254740994"))
                .andExpect(jsonPath("$.data.list[0].orderId").value("9007199254740995"));

        ArgumentCaptor<ProductReviewAdminListRequest> request =
                ArgumentCaptor.forClass(ProductReviewAdminListRequest.class);
        verify(reviewService).adminList(request.capture());
        assertEquals(301L, request.getValue().getProductId());
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0),
                request.getValue().getCreatedAtStart());
    }

    @Test
    void hideRestoreAndValidationRoutesWork() throws Exception {
        when(reviewService.hide(org.mockito.ArgumentMatchers.eq(88L), any())).thenReturn(true);
        when(reviewService.restore(88L)).thenReturn(true);

        mockMvc.perform(put("/admin/review/88/hide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"内容不符合规范"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(put("/admin/review/88/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(put("/admin/review/89/hide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"a"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));
    }
}
