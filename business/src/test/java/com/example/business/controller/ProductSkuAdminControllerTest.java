package com.example.business.controller;

import com.example.business.controller.admin.ProductSkuAdminController;
import com.example.business.response.ProductSkuGenerateResponse;
import com.example.business.service.ProductSkuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductSkuAdminControllerTest {
    @Mock
    private ProductSkuService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductSkuAdminController(service)).build();
    }

    @Test
    void generateRouteReturnsCreatedAndReusedCounts() throws Exception {
        ProductSkuGenerateResponse response = new ProductSkuGenerateResponse();
        response.setGeneratedCount(6);
        response.setReusedCount(0);
        when(service.generate(any())).thenReturn(response);

        mockMvc.perform(post("/admin/product-sku/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 7,
                                  "defaultPrice": 12.00,
                                  "defaultStock": 10,
                                  "selections": [
                                    {"attributeId": 11, "attributeValueIds": [101, 102, 103]},
                                    {"attributeId": 12, "attributeValueIds": [201, 202]}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.generatedCount").value(6));
    }

    @Test
    void deleteRouteRequiresProductOwnershipParameter() throws Exception {
        when(service.deleteSku(7L, 9L)).thenReturn(true);

        mockMvc.perform(delete("/admin/product-sku/9").param("productId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void generateRouteAllowsNoSpecProductDefaultSku() throws Exception {
        ProductSkuGenerateResponse response = new ProductSkuGenerateResponse();
        response.setGeneratedCount(1);
        when(service.generate(any())).thenReturn(response);

        mockMvc.perform(post("/admin/product-sku/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 7,
                                  "defaultPrice": 12.00,
                                  "defaultStock": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(1));
    }
}
