package com.example.business.controller;

import com.example.business.controller.admin.ProductAttributeAdminController;
import com.example.business.response.ProductAttributeResponse;
import com.example.business.service.ProductAttributeService;
import com.example.common.response.R;
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
class ProductAttributeAdminControllerTest {
    @Mock
    private ProductAttributeService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductAttributeAdminController(service)).build();
    }

    @Test
    void treeRouteReturnsProductAttributeTree() throws Exception {
        ProductAttributeResponse response = new ProductAttributeResponse();
        response.setId(11L);
        response.setName("口味");
        when(service.listTree(7L)).thenReturn(List.of(response));

        mockMvc.perform(get("/admin/product-attribute/tree").param("productId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("口味"));
    }
}
