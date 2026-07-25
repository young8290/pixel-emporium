package com.example.business.controller;

import com.example.business.request.CartAddRequest;
import com.example.business.request.CartUpdateRequest;
import com.example.business.response.CartViewResponse;
import com.example.business.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {
    @Mock
    private CartService cartService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CartController(cartService))
                .setValidator(validator)
                .build();
    }

    @Test
    void getCartReturnsCurrentView() throws Exception {
        when(cartService.getCart()).thenReturn(new CartViewResponse());

        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void addDelegatesValidRequest() throws Exception {
        when(cartService.add(any(CartAddRequest.class))).thenReturn(true);

        mockMvc.perform(post("/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productSkuId":70,"quantity":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void updateDelegatesValidRequest() throws Exception {
        when(cartService.update(any(CartUpdateRequest.class))).thenReturn(true);

        mockMvc.perform(put("/cart/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":90,"quantity":3,"checked":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void deleteDelegatesOwnedItemId() throws Exception {
        when(cartService.delete(90L)).thenReturn(true);

        mockMvc.perform(delete("/cart/90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void batchDeleteDelegatesIds() throws Exception {
        when(cartService.deleteBatch(List.of(90L, 91L))).thenReturn(true);

        mockMvc.perform(delete("/cart/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[90,91]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void clearDelegatesCurrentUserCart() throws Exception {
        when(cartService.clear()).thenReturn(true);

        mockMvc.perform(delete("/cart/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void addRejectsNonPositiveQuantityAtHttpBoundary() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productSkuId":70,"quantity":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRejectsMissingCartItemIdAtHttpBoundary() throws Exception {
        mockMvc.perform(put("/cart/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"checked":true}
                                """))
                .andExpect(status().isBadRequest());
    }
}
