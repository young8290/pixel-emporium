package com.example.business.controller;

import com.example.business.enums.OrderStatus;
import com.example.business.enums.PaymentStatus;
import com.example.business.response.AlipayPaymentResponse;
import com.example.business.response.PaymentResponse;
import com.example.business.service.AlipayPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AlipayPayControllerTest {
    @Mock private AlipayPaymentService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AlipayPayController(service)).build();
    }

    @Test
    void createsPagePaymentAndQueriesServerStatus() throws Exception {
        when(service.create("ORDER1")).thenReturn(new AlipayPaymentResponse(
                "PAY1", "ORDER1", new BigDecimal("12.00"), "<form>pay</form>"));
        PaymentResponse statusResponse = new PaymentResponse();
        statusResponse.setPayNo("PAY1");
        statusResponse.setStatus(PaymentStatus.SUCCESS);
        statusResponse.setOrderStatus(OrderStatus.PAID);
        when(service.query("ORDER1")).thenReturn(statusResponse);

        mockMvc.perform(post("/pay/alipay/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNo\":\"ORDER1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payNo").value("PAY1"))
                .andExpect(jsonPath("$.data.formHtml").value("<form>pay</form>"));

        mockMvc.perform(post("/pay/alipay/query").param("orderNo", "ORDER1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void returnsOnlyAlipaySuccessOrFailureTextForNotifications() throws Exception {
        when(service.notify(anyMap())).thenReturn(true, false);

        mockMvc.perform(post("/pay/alipay/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("out_trade_no", "PAY1")
                        .param("trade_status", "TRADE_SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("success"));

        mockMvc.perform(post("/pay/alipay/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("out_trade_no", "PAY1"))
                .andExpect(status().isOk())
                .andExpect(content().string("failure"));
    }
}
