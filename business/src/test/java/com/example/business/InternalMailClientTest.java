package com.example.business;

import com.example.business.client.InternalMailClient;
import com.example.business.config.MailNotificationProperties;
import com.example.business.model.OrderCreatedMailEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalMailClientTest {
    @Test
    void refusesToSendWhenHmacSecretIsMissingOrTooShort() {
        InternalMailClient missing = client("http://localhost", "", 100);
        InternalMailClient shortSecret = client("http://localhost", "short", 100);

        assertFalse(missing.validSecret());
        assertFalse(shortSecret.validSecret());
        assertThrows(IllegalStateException.class,
                () -> missing.post(java.util.Map.of(), "ORDER_CREATED:1"));
    }

    @Test
    void signsRequestAndSerializesLongIdentifiersAsJsonStrings() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> eventHeader = new AtomicReference<>();
        AtomicReference<String> signatureHeader = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/mail/order-created", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            eventHeader.set(exchange.getRequestHeaders().getFirst("X-Internal-Event-Id"));
            signatureHeader.set(exchange.getRequestHeaders().getFirst("X-Internal-Signature"));
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            OrderCreatedMailEvent event = new OrderCreatedMailEvent();
            event.setEventId("ORDER_CREATED:9007199254740993");
            event.setOrderId(9_007_199_254_740_993L);
            event.setUserId(9_007_199_254_740_995L);

            int status = client(baseUrl, secret(), 2_000).post(event, event.getEventId());

            assertEquals(202, status);
            assertEquals(event.getEventId(), eventHeader.get());
            assertTrue(signatureHeader.get().matches("[0-9a-f]{64}"));
            assertTrue(body.get().contains("\"orderId\":\"9007199254740993\""));
            assertTrue(body.get().contains("\"userId\":\"9007199254740995\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void appliesPerRequestReadTimeout() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/mail/order-created", exchange -> {
            try {
                Thread.sleep(500);
                exchange.sendResponseHeaders(202, -1);
            } catch (Exception ignored) {
                // The client is expected to close the timed-out exchange.
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            assertThrows(HttpTimeoutException.class,
                    () -> client(baseUrl, secret(), 100).post(java.util.Map.of("id", 1), "ORDER_CREATED:1"));
        } finally {
            server.stop(0);
        }
    }

    private InternalMailClient client(String baseUrl, String secret, long readTimeoutMs) {
        return new InternalMailClient(new MailNotificationProperties(
                baseUrl, secret, 1_000, readTimeoutMs, 10_000, 20), new ObjectMapper());
    }

    private String secret() {
        return "01234567890123456789012345678901";
    }
}
