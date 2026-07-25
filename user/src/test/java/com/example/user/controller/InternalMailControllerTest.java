package com.example.user.controller;

import com.example.common.advice.GlobalExceptionHandler;
import com.example.user.model.OrderCreatedMailEvent;
import com.example.user.security.InternalMailRequestVerifier;
import com.example.user.service.MailDeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalMailControllerTest {
    @Test
    void duplicateEventReturnsOkInsteadOfAccepted() throws Exception {
        InternalMailRequestVerifier verifier = mock(InternalMailRequestVerifier.class);
        MailDeliveryService delivery = mock(MailDeliveryService.class);
        doNothing().when(verifier).verify(any(Long.class), any(String.class), any(String.class), any(byte[].class));
        when(delivery.accept(any(OrderCreatedMailEvent.class))).thenReturn(false);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new InternalMailController(verifier, new ObjectMapper(), delivery))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/internal/mail/order-created")
                        .header("X-Internal-Event-Id", "ORDER_CREATED:1")
                        .header("X-Internal-Timestamp", 1L)
                        .header("X-Internal-Signature", "signature")
                        .contentType("application/json")
                        .content("{\"eventId\":\"ORDER_CREATED:1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsWhenHeaderEventIdDiffersFromVerifiedBody() throws Exception {
        InternalMailRequestVerifier verifier = mock(InternalMailRequestVerifier.class);
        MailDeliveryService delivery = mock(MailDeliveryService.class);
        doNothing().when(verifier).verify(any(Long.class), any(String.class), any(String.class), any(byte[].class));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new InternalMailController(verifier, new ObjectMapper(), delivery))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/internal/mail/order-created")
                        .header("X-Internal-Event-Id", "ORDER_CREATED:1")
                        .header("X-Internal-Timestamp", 1L)
                        .header("X-Internal-Signature", "signature")
                        .contentType("application/json")
                        .content("{\"eventId\":\"ORDER_CREATED:2\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
