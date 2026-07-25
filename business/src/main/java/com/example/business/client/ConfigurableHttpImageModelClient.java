package com.example.business.client;

import com.example.business.config.PixelStyleProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

@Component
public class ConfigurableHttpImageModelClient implements ImageModelClient {
    private final PixelStyleProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public ConfigurableHttpImageModelClient(PixelStyleProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1, properties.getTimeoutMs())))
                .build());
    }

    ConfigurableHttpImageModelClient(PixelStyleProperties properties, ObjectMapper objectMapper,
                                     HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public ImageModelResult imageToImage(ImageModelRequest request) {
        PixelStyleProperties.Api api = properties.getApi();
        URI endpoint = endpoint(api.getBaseUrl());
        ObjectNode body = objectMapper.createObjectNode();
        body.put(api.getImageField(), Base64.getEncoder().encodeToString(request.imageBytes()));
        body.put(api.getPromptField(), request.prompt());
        body.put(api.getNegativeField(), request.negativePrompt());
        body.put(api.getStrengthField(), request.strength());
        body.put(api.getSizeField(), request.size());
        if (StringUtils.hasText(api.getModel())) {
            body.put(api.getModelField(), api.getModel());
        }

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(body);
        } catch (Exception exception) {
            throw new ImageModelException(ImageModelException.Reason.INVALID_RESPONSE, "模型请求组装失败", exception);
        }
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMillis(Math.max(1, properties.getTimeoutMs())))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));
        if (StringUtils.hasText(api.getKey())) {
            String value = StringUtils.hasText(api.getAuthScheme())
                    ? api.getAuthScheme().trim() + " " + api.getKey().trim() : api.getKey().trim();
            requestBuilder.header(api.getAuthHeader(), value);
        }

        int maxAttempts = Math.min(6, Math.max(1, properties.getMaxRetries() + 1));
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status == 401 || status == 403) {
                    throw new ImageModelException(ImageModelException.Reason.AUTHENTICATION, "模型鉴权失败");
                }
                if (status == 429) {
                    throw new ImageModelException(ImageModelException.Reason.QUOTA, "模型调用配额不足");
                }
                if (status >= 500 || status == 408) {
                    if (attempt < maxAttempts) {
                        continue;
                    }
                    throw new ImageModelException(ImageModelException.Reason.PROVIDER, "模型服务暂时不可用");
                }
                if (status < 200 || status >= 300) {
                    throw new ImageModelException(ImageModelException.Reason.PROVIDER, "模型请求失败");
                }
                return parseResult(response.body(), api);
            } catch (ImageModelException exception) {
                throw exception;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ImageModelException(ImageModelException.Reason.TIMEOUT, "模型请求被中断", exception);
            } catch (java.io.IOException exception) {
                if (attempt < maxAttempts) {
                    continue;
                }
                throw new ImageModelException(ImageModelException.Reason.TIMEOUT, "模型请求超时", exception);
            }
        }
        throw new ImageModelException(ImageModelException.Reason.PROVIDER, "模型请求失败");
    }

    private ImageModelResult parseResult(String body, PixelStyleProperties.Api api) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode base64 = nodeAt(root, api.getResponseBase64Field());
            if (base64 != null && base64.isTextual() && !base64.asText().isBlank()) {
                byte[] bytes = Base64.getDecoder().decode(base64.asText());
                JsonNode contentType = nodeAt(root, api.getResponseContentTypeField());
                return new ImageModelResult(bytes,
                        contentType != null && contentType.isTextual() ? contentType.asText() : "image/png",
                        null, "styled.png");
            }
            JsonNode url = nodeAt(root, api.getResponseUrlField());
            if (url != null && url.isTextual()) {
                URI resultUri = URI.create(url.asText());
                if (!("http".equalsIgnoreCase(resultUri.getScheme())
                        || "https".equalsIgnoreCase(resultUri.getScheme()))
                        || resultUri.getUserInfo() != null) {
                    throw new ImageModelException(ImageModelException.Reason.INVALID_RESPONSE, "模型结果地址无效");
                }
                return new ImageModelResult(null, null, resultUri, "styled.png");
            }
        } catch (ImageModelException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ImageModelException(ImageModelException.Reason.INVALID_RESPONSE, "模型结果无效", exception);
        }
        throw new ImageModelException(ImageModelException.Reason.INVALID_RESPONSE, "模型未返回图片");
    }

    private JsonNode nodeAt(JsonNode root, String path) {
        if (root == null || !StringUtils.hasText(path)) {
            return null;
        }
        JsonNode node = root;
        for (String token : path.replace("[", ".").replace("]", "").split("\\.")) {
            if (token.isBlank()) {
                continue;
            }
            if (node == null) {
                return null;
            }
            node = token.chars().allMatch(Character::isDigit)
                    ? node.isArray() && Integer.parseInt(token) < node.size() ? node.get(Integer.parseInt(token)) : null
                    : node.get(token);
        }
        return node;
    }

    private URI endpoint(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (RuntimeException exception) {
            throw new ImageModelException(ImageModelException.Reason.PROVIDER, "模型地址配置无效");
        }
    }
}
