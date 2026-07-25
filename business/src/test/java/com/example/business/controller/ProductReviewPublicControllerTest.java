package com.example.business.controller;

import com.example.business.response.ProductReviewResponse;
import com.example.business.response.ProductReviewSummaryResponse;
import com.example.business.service.ProductReviewService;
import com.example.business.service.ProductService;
import com.example.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductReviewPublicControllerTest {
    private ProductReviewService reviewService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductService productService = mock(ProductService.class);
        reviewService = mock(ProductReviewService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ProductController(productService, reviewService)).build();
    }

    @Test
    void publicReviewListAndSummaryNeedNoAuthorizationHeader() throws Exception {
        PageResponse<ProductReviewResponse> page = new PageResponse<>();
        page.setTotal(0L);
        page.setList(List.of());
        when(reviewService.publicList(org.mockito.ArgumentMatchers.eq(7L), any())).thenReturn(page);

        ProductReviewSummaryResponse summary = new ProductReviewSummaryResponse();
        summary.setProductId(7L);
        summary.setAverageRating(new BigDecimal("0.00"));
        summary.setReviewCount(0L);
        summary.setDistribution(new LinkedHashMap<>());
        when(reviewService.summary(7L)).thenReturn(summary);

        mockMvc.perform(get("/product/7/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/product/7/review-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value("7"));
    }
}
