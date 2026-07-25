package com.example.business.controller;

import com.example.business.request.OrderCreateRequest;
import com.example.business.request.OrderListRequest;
import com.example.business.response.OrderDetailResponse;
import com.example.business.response.OrderResponse;
import com.example.business.service.OrderService;
import com.example.common.advice.GlobalExceptionHandler;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomForbiddenException;
import com.example.common.response.PageResponse;
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
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {
    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createDelegatesValidBuyNowRequest() throws Exception {
        when(orderService.create(any(OrderCreateRequest.class))).thenReturn("202607231001");

        mockMvc.perform(post("/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addressId":1,"source":"buyNow","skuId":70,"quantity":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("202607231001"));
    }

    @Test
    void listAndDetailSerializeLongIdsAsStrings() throws Exception {
        OrderResponse row = new OrderResponse();
        row.setId(9007199254740993L);
        row.setOrderNo("NO1");
        PageResponse<OrderResponse> page = new PageResponse<>();
        page.setTotal(1L);
        page.setList(List.of(row));
        when(orderService.list(any(OrderListRequest.class))).thenReturn(page);
        OrderDetailResponse detail = new OrderDetailResponse();
        detail.setId(9007199254740993L);
        detail.setOrderNo("NO1");
        when(orderService.detail(9007199254740993L)).thenReturn(detail);

        mockMvc.perform(get("/order/list")
                        .param("page", "1")
                        .param("size", "10")
                        .param("status", "PENDING_PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value("9007199254740993"));

        mockMvc.perform(get("/order/9007199254740993"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("9007199254740993"));
    }

    @Test
    void cancelAndConfirmDelegatePathIds() throws Exception {
        when(orderService.cancel(501L)).thenReturn(true);
        when(orderService.confirm(502L)).thenReturn(true);

        mockMvc.perform(put("/order/501/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(put("/order/502/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void invalidCreateRequestReturns422WithoutCallingService() throws Exception {
        mockMvc.perform(post("/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"source":"buyNow","skuId":70,"quantity":0}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    void commonExceptionsMapTo404403And409() throws Exception {
        when(orderService.detail(501L)).thenThrow(new NoSuchElementException("订单不存在"));
        when(orderService.detail(502L)).thenThrow(new CustomForbiddenException("无权查看"));
        when(orderService.cancel(503L)).thenThrow(new CustomConflictException("状态冲突"));

        mockMvc.perform(get("/order/501"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(get("/order/502"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(put("/order/503/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }
}
