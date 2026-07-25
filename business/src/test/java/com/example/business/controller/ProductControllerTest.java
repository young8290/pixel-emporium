package com.example.business.controller;

import com.example.business.response.ProductDetailResponse;
import com.example.business.response.ProductResponse;
import com.example.business.service.ProductService;
import com.example.business.service.ProductReviewService;
import com.example.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {
    @Mock
    private ProductService productService;

    @Mock
    private ProductReviewService productReviewService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ProductController(productService, productReviewService)).build();
    }

    @Test
    void publicListDoesNotRequireAuthorizationHeader() throws Exception {
        PageResponse<ProductResponse> page = new PageResponse<>();
        page.setTotal(0L);
        page.setList(List.of());
        when(productService.getPublicProductPage(org.mockito.ArgumentMatchers.any()))
                .thenReturn(page);

        mockMvc.perform(get("/product/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void publicDetailRouteIsExposed() throws Exception {
        ProductDetailResponse detail = new ProductDetailResponse();
        when(productService.getPublicProductDetail(7L)).thenReturn(detail);

        mockMvc.perform(get("/product/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void publicSuggestionsRouteTrimsKeywordAndCapsLimit() throws Exception {
        when(productService.getSearchSuggestions("灯", 10)).thenReturn(List.of("月光台灯"));

        mockMvc.perform(get("/product/suggestions")
                        .param("keyword", "  灯  ")
                        .param("limit", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("月光台灯"));
    }

    @Test
    void publicRecommendationsRouteCapsLimit() throws Exception {
        ProductResponse recommendation = new ProductResponse();
        recommendation.setId(8L);
        when(productService.getRecommendations(7L, 20)).thenReturn(List.of(recommendation));

        mockMvc.perform(get("/product/7/recommendations").param("limit", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("8"));
    }
}
