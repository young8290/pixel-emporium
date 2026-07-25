package com.example.business.controller;

import com.example.business.controller.admin.OrderAdminController;
import com.example.business.request.OrderAdminListRequest;
import com.example.business.response.OrderAdminDetailResponse;
import com.example.business.response.OrderAdminResponse;
import com.example.business.service.OrderService;
import com.example.common.advice.GlobalExceptionHandler;
import com.example.common.annotation.CustomRequireRole;
import com.example.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
class OrderAdminControllerTest {
    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderAdminController(orderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void controllerRequiresAdminOrSuperAdmin() {
        CustomRequireRole annotation = OrderAdminController.class
                .getAnnotation(CustomRequireRole.class);

        assertNotNull(annotation);
        assertEquals(Set.of("ADMIN", "SUPER_ADMIN"), Set.of(annotation.value()));
    }

    @Test
    void listBindsAllFiltersAndSerializesIdsAsStrings() throws Exception {
        OrderAdminResponse row = new OrderAdminResponse();
        row.setId(9007199254740993L);
        row.setUserId(9007199254740994L);
        PageResponse<OrderAdminResponse> page = new PageResponse<>();
        page.setTotal(1L);
        page.setList(List.of(row));
        when(orderService.adminList(any())).thenReturn(page);

        mockMvc.perform(get("/admin/order/list")
                        .param("orderNo", "NO50")
                        .param("userId", "11")
                        .param("status", "PAID")
                        .param("createdAtStart", "2026-07-01T00:00:00")
                        .param("createdAtEnd", "2026-07-31T23:59:59")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].id").value("9007199254740993"))
                .andExpect(jsonPath("$.data.list[0].userId").value("9007199254740994"));

        ArgumentCaptor<OrderAdminListRequest> request =
                ArgumentCaptor.forClass(OrderAdminListRequest.class);
        verify(orderService).adminList(request.capture());
        assertEquals("NO50", request.getValue().getOrderNo());
        assertEquals(11L, request.getValue().getUserId());
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0),
                request.getValue().getCreatedAtStart());
        assertEquals(LocalDateTime.of(2026, 7, 31, 23, 59, 59),
                request.getValue().getCreatedAtEnd());
        assertEquals(1, request.getValue().getPage());
        assertEquals(10, request.getValue().getSize());
    }

    @Test
    void detailAndShipDelegatePathId() throws Exception {
        OrderAdminDetailResponse detail = new OrderAdminDetailResponse();
        detail.setId(501L);
        when(orderService.adminDetail(501L)).thenReturn(detail);
        when(orderService.ship(org.mockito.ArgumentMatchers.eq(501L), any())).thenReturn(true);

        mockMvc.perform(get("/admin/order/501"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("501"));
        mockMvc.perform(put("/admin/order/501/ship")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"shippingCarrier\":\"像素快递\",\"trackingNumber\":\"PX001\",\"shippingNote\":\"轻放\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(orderService).adminDetail(501L);
        verify(orderService).ship(org.mockito.ArgumentMatchers.eq(501L), any());
    }
}
