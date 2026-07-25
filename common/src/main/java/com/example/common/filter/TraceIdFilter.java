package com.example.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceIdFilter：为每个请求建立贯穿日志的 traceId（BE-P2-01）。
 *
 * <p>规则：
 * <ul>
 *   <li>优先透传上游（网关）注入的 {@code X-Trace-Id}；缺失时本地生成一个。</li>
 *   <li>放入 SLF4J {@link MDC}，logback pattern 通过 {@code %X{traceId}} 打印。</li>
 *   <li>响应头回写 {@code X-Trace-Id}，便于前端/排查关联。</li>
 *   <li>请求结束务必清理 MDC，避免线程复用串号。</li>
 * </ul>
 *
 * <p>最高优先级执行，保证后续所有过滤器/业务日志都带上 traceId。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
