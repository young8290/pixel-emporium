package com.example.business.controller;

import com.example.business.enums.OrderStatus;
import com.example.business.enums.PaymentStatus;
import com.example.business.response.PaymentResponse;
import com.example.business.service.PaymentService;
import com.example.common.advice.GlobalExceptionHandler;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PayControllerTest {
    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new PayController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void exposesCreateCallbackAndStatusContracts() throws Exception {
        PaymentResponse response = pendingResponse();
        when(paymentService.create("ORDER1")).thenReturn(response);
        when(paymentService.callback(eq("PAY1"), any(BigDecimal.class))).thenReturn(true);
        when(paymentService.status("ORDER1")).thenReturn(response);

        mockMvc.perform(post("/pay/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"ORDER1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.payNo").value("PAY1"))
                .andExpect(jsonPath("$.data.orderNo").value("ORDER1"))
                .andExpect(jsonPath("$.data.amount").value(12.00))
                .andExpect(jsonPath("$.data.channel").value("MOCK"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.mockPayment").value(true))
                .andExpect(jsonPath("$.data.confirmable").value(true));

        mockMvc.perform(post("/pay/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payNo":"PAY1","amount":12.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/pay/status").param("orderNo", "ORDER1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payNo").value("PAY1"))
                .andExpect(jsonPath("$.data.confirmable").value(true));
    }

    @Test
    void rejectsInvalidCreateAndCallbackBodiesAtHttpBoundary() throws Exception {
        mockMvc.perform(post("/pay/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":" "}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));

        mockMvc.perform(post("/pay/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payNo":"PAY1","amount":-0.01}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));

        verifyNoInteractions(paymentService);
    }

    @Test
    void requiresOrderNumberQueryParameter() throws Exception {
        mockMvc.perform(get("/pay/status"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsPaymentExceptionsTo404403And409() throws Exception {
        when(paymentService.create("MISSING"))
                .thenThrow(new NoSuchElementException("订单不存在"));
        when(paymentService.status("FOREIGN"))
                .thenThrow(new CustomForbiddenException("无权操作该订单"));
        when(paymentService.callback(eq("PAY1"), any(BigDecimal.class)))
                .thenThrow(new CustomConflictException("支付金额不一致"));

        mockMvc.perform(post("/pay/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"MISSING"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(get("/pay/status").param("orderNo", "FOREIGN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(post("/pay/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payNo":"PAY1","amount":12.00}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void disablesLegacyMockCallbackWhenConfiguredOff() throws Exception {
        MockMvc disabled = MockMvcBuilders.standaloneSetup(new PayController(paymentService, false))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        disabled.perform(post("/pay/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"payNo\":\"PAY1\",\"amount\":12.00}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(paymentService);
    }

    private PaymentResponse pendingResponse() {
        PaymentResponse response = new PaymentResponse();
        response.setPayNo("PAY1");
        response.setOrderNo("ORDER1");
        response.setAmount(new BigDecimal("12.00"));
        response.setChannel("MOCK");
        response.setStatus(PaymentStatus.PENDING);
        response.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        response.setMockPayment(true);
        response.setConfirmable(true);
        return response;
    }
}
