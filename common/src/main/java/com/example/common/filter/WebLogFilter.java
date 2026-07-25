package com.example.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * WebLogFilter 记录每个 HTTP 请求/响应的摘要日志（BE-P2-01）。
 *
 * <p>核心功能：
 * <ul>
 *   <li>traceId 由 {@link TraceIdFilter} 放入 MDC，logback pattern 通过 {@code %X{traceId}} 打印；
 *       本过滤器不再自行生成，保证同一请求日志 traceId 一致。</li>
 *   <li>ContentCachingWrapper 包装请求/响应，允许重复读取 body。</li>
 *   <li>脱敏：不打印 Authorization 头、请求/响应体中的密码/token/密钥等敏感字段。</li>
 *   <li>只记录文本类 Content-Type，二进制（图片/文件）不打印。</li>
 *   <li>慢请求（超过阈值）单独 warn。</li>
 * </ul>
 *
 * <p>排在 {@link TraceIdFilter} 之后，保证日志已带 traceId。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class WebLogFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(WebLogFilter.class);

    /** 慢请求阈值（毫秒），超过则 warn。 */
    private static final long SLOW_REQUEST_THRESHOLD_MS = 2000L;

    /** 请求体最大记录长度。 */
    private static final int MAX_BODY_LENGTH = 2000;

    /**
     * 敏感字段脱敏：匹配 JSON 或表单里的 password/token/secret/accessKey 等值，替换为 ***。
     * 覆盖 "password":"xxx"、password=xxx 两种形态。
     */
    private static final Pattern SENSITIVE_JSON = Pattern.compile(
            "(?i)(\"(?:password|pwd|oldPassword|newPassword|token|accessToken|refreshToken|secret|secretKey|accessKey|access-key|secret-key|authorization)\"\\s*:\\s*\")[^\"]*(\")");
    private static final Pattern SENSITIVE_FORM = Pattern.compile(
            "(?i)((?:password|pwd|token|secret|secretKey|accessKey)=)[^&\\s]*");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            logRequest(requestWrapper);
            logResponse(responseWrapper, cost);
            if (cost > SLOW_REQUEST_THRESHOLD_MS) {
                logger.warn("Slow request [URL: {}, Method: {}, Cost: {}ms]",
                        requestWrapper.getRequestURI(), requestWrapper.getMethod(), cost);
            }
            // 将缓存的响应内容复制回原始响应流，否则客户端将收到空响应
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String body = getPayload(request.getContentAsByteArray(), request.getContentType());
        logger.info("Request [URL: {}, Method: {}, IP: {}, Body: {}]",
                request.getRequestURI(),
                request.getMethod(),
                request.getRemoteAddr(),
                body);
    }

    private void logResponse(ContentCachingResponseWrapper response, long cost) {
        String body = getPayload(response.getContentAsByteArray(), response.getContentType());
        logger.info("Response [Status: {}, Spend Time: {}ms, Body: {}]",
                response.getStatus(),
                cost,
                body);
    }

    private String getPayload(byte[] content, String contentType) {
        if (content.length == 0) {
            return "";
        }
        if (!isCompatibleContentType(contentType)) {
            return "[Binary Content or Non-Text File]";
        }
        String body = new String(content, StandardCharsets.UTF_8);
        return truncate(desensitize(body));
    }

    /**
     * 脱敏：抹掉请求/响应体中的密码、token、密钥等敏感值。
     */
    private String desensitize(String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        String masked = SENSITIVE_JSON.matcher(body).replaceAll("$1***$2");
        masked = SENSITIVE_FORM.matcher(masked).replaceAll("$1***");
        return masked;
    }

    private boolean isCompatibleContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.contains("application/json")
                || contentType.contains("application/xml")
                || contentType.contains("text/")
                || contentType.contains("application/x-www-form-urlencoded");
    }

    private String truncate(String str) {
        if (str == null) {
            return "";
        }
        str = str.replaceAll("[\r\n]", "");
        if (str.length() > MAX_BODY_LENGTH) {
            return str.substring(0, MAX_BODY_LENGTH) + "...(truncated)";
        }
        return str;
    }
}
