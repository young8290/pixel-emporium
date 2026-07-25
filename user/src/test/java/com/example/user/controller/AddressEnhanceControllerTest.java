package com.example.user.controller;

import com.example.common.advice.GlobalExceptionHandler;
import com.example.user.request.AddressRequest;
import com.example.user.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AddressEnhanceControllerTest {
    private AddressService addressService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        addressService = mock(AddressService.class);
        AddressController controller = new AddressController();
        ReflectionTestUtils.setField(controller, "addressService", addressService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void setDefaultDelegatesToAddressService() throws Exception {
        when(addressService.setDefault(12L)).thenReturn(true);

        mockMvc.perform(put("/address/default/{id}", 12L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        verify(addressService).setDefault(12L);
    }

    @Test
    void saveRejectsMissingStructuredFields() throws Exception {
        mockMvc.perform(post("/address/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"广东省深圳市南山区\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));

        verifyNoInteractions(addressService);
    }

    @Test
    void saveDelegatesValidStructuredAddress() throws Exception {
        when(addressService.saveOrUpdate(any(AddressRequest.class))).thenReturn(true);

        mockMvc.perform(post("/address/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipient": "张三",
                                  "phone": "13800138000",
                                  "province": "广东省",
                                  "city": "深圳市",
                                  "district": "南山区",
                                  "detail": "科技南十二路",
                                  "isDefault": true,
                                  "userId": 99
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        ArgumentCaptor<AddressRequest> request = ArgumentCaptor.forClass(AddressRequest.class);
        verify(addressService).saveOrUpdate(request.capture());
        assertEquals("张三", request.getValue().getRecipient());
        assertEquals("13800138000", request.getValue().getPhone());
        assertTrue(request.getValue().getIsDefault());
        assertEquals(99L, request.getValue().getUserId());
    }
}
