package com.example.business.client;

import com.example.business.config.PixelStyleProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigurableHttpImageModelClientTest {
    @Test
    void mapsConfiguredResponseBase64AndAuthorization() throws Exception {
        PixelStyleProperties properties = properties();
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        String encoded = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        when(response.body()).thenReturn("{\"data\":[{\"b64_json\":\"" + encoded + "\"}]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        ConfigurableHttpImageModelClient client = new ConfigurableHttpImageModelClient(
                properties, new ObjectMapper(), httpClient);
        ImageModelClient.ImageModelResult result = client.imageToImage(
                new ImageModelClient.ImageModelRequest(new byte[]{9}, "image/png", "prompt", "negative", 0.42, 512));

        assertArrayEquals(new byte[]{1, 2, 3}, result.bytes());
        assertEquals("image/png", result.contentType());
        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("Bearer test-key", request.getValue().headers().firstValue("Authorization").orElseThrow());
        assertEquals(URI.create("https://model.example.test/v1"), request.getValue().uri());
    }

    @Test
    void retriesTransientProviderFailureWithBoundedAttempts() throws Exception {
        PixelStyleProperties properties = properties();
        properties.setMaxRetries(1);
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> failed = mock(HttpResponse.class);
        HttpResponse<String> success = mock(HttpResponse.class);
        when(failed.statusCode()).thenReturn(503);
        when(success.statusCode()).thenReturn(200);
        when(success.body()).thenReturn("{\"data\":[{\"url\":\"https://cdn.example.test/styled.png\"}]}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(failed, success);

        ConfigurableHttpImageModelClient client = new ConfigurableHttpImageModelClient(
                properties, new ObjectMapper(), httpClient);
        ImageModelClient.ImageModelResult result = client.imageToImage(
                new ImageModelClient.ImageModelRequest(new byte[]{9}, "image/png", "prompt", "negative", 0.42, 512));

        assertEquals(URI.create("https://cdn.example.test/styled.png"), result.url());
        verify(httpClient, org.mockito.Mockito.times(2))
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    private static PixelStyleProperties properties() {
        PixelStyleProperties properties = new PixelStyleProperties();
        properties.setMaxRetries(0);
        properties.getApi().setBaseUrl("https://model.example.test/v1");
        properties.getApi().setKey("test-key");
        return properties;
    }
}
