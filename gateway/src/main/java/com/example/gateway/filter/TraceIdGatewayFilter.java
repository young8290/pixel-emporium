package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * TraceIdGatewayFilter：在网关入口生成/透传 traceId（BE-P2-01）。
 *
 * <p>网关是链路的最上游，这里生成 {@code X-Trace-Id} 并写入转发给下游（user/business）的请求头，
 * 下游 {@code TraceIdFilter} 优先透传该值，实现一次请求一个贯穿 traceId。
 * 同时把 traceId 回写到响应头，便于前端/排查关联。
 *
 * <p>以最高优先级执行，保证在鉴权等其它过滤器之前建立 traceId。
 */
@Component
public class TraceIdGatewayFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        final String finalTraceId = traceId;

        // 透传给下游服务
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, finalTraceId)
                .build();
        // 回写响应头
        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, finalTraceId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // 高于 BearerTokenFilter(0)，最先执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
