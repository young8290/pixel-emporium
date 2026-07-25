package com.example.business.controller;

import com.example.business.request.ProductReviewCreateRequest;
import com.example.business.request.ProductReviewUpdateRequest;
import com.example.business.response.OrderReviewEligibilityResponse;
import com.example.business.service.ProductReviewService;
import com.example.common.advice.GlobalExceptionHandler;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomUnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductReviewControllerTest {
    @Mock
    private ProductReviewService reviewService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductReviewController(reviewService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createUpdateDeleteAndEligibilityRoutesDelegateWithStringIds() throws Exception {
        when(reviewService.create(any(ProductReviewCreateRequest.class))).thenReturn(null);
        when(reviewService.update(eq(9007199254740993L), any(ProductReviewUpdateRequest.class)))
                .thenReturn(null);
        when(reviewService.delete(9007199254740993L)).thenReturn(true);
        OrderReviewEligibilityResponse eligibility = new OrderReviewEligibilityResponse();
        eligibility.setOrderId(9007199254740993L);
        when(reviewService.orderEligibility(9007199254740993L)).thenReturn(eligibility);

        mockMvc.perform(post("/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderItemId":"701","rating":5,"content":"已经购买并使用，体验很好"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(put("/review/9007199254740993")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":4,"content":"修改后的有效评价"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/review/9007199254740993"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(get("/review/order/9007199254740993"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value("9007199254740993"));
    }

    @Test
    void invalidCreateRequestIsRejectedBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderItemId":"701","rating":6,"content":"短"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));
        verify(reviewService, never()).create(any());
    }

    @Test
    void serviceExceptionsMapToUnauthorizedAndConflict() throws Exception {
        when(reviewService.delete(701L)).thenThrow(new CustomUnauthorizedException("请先登录"));
        when(reviewService.update(eq(702L), any(ProductReviewUpdateRequest.class)))
                .thenThrow(new CustomConflictException("评价状态已变化"));

        mockMvc.perform(delete("/review/701"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(put("/review/702")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":4,"content":"修改后的有效评价"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }
}
