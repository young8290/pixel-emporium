package com.example.gateway.filter;

import com.example.common.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class BearerTokenFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(BearerTokenFilter.class);

    private final ReactiveTokenStateChecker tokenStateChecker;

    @Value("${auth.jwt.secret-key}")
    private String secretKey;

    public BearerTokenFilter(ReactiveTokenStateChecker tokenStateChecker) {
        this.tokenStateChecker = tokenStateChecker;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7) : null;
        if (token == null || token.isBlank() || !validateToken(token)) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        String validatedToken = token;
        Mono<Boolean> tokenState = tokenStateChecker.isBlacklisted(validatedToken)
                .onErrorResume(exception -> {
                    log.error("令牌状态检查失败，拒绝转发 {}", path,
                            exception);
                    return reject(exchange, HttpStatus.SERVICE_UNAVAILABLE)
                            .then(Mono.<Boolean>empty());
                });
        return tokenState
                .flatMap(revoked -> revoked
                        ? reject(exchange, HttpStatus.UNAUTHORIZED)
                        : authorizeAndForward(exchange, chain, path, validatedToken));
    }

    private Mono<Void> authorizeAndForward(ServerWebExchange exchange, GatewayFilterChain chain,
                                           String path, String token) {
        if (path.contains("/admin/")) {
            try {
                var roles = JwtUtil.getRoles(token);
                if (!roles.contains("SUPER_ADMIN") && !roles.contains("ADMIN")) {
                    return reject(exchange, HttpStatus.FORBIDDEN);
                }
            } catch (Exception exception) {
                log.error("网关解析令牌角色失败，拒绝请求 {}", path, exception);
                return reject(exchange, HttpStatus.FORBIDDEN);
            }
        }
        return chain.filter(exchange);
    }

    private boolean isPublicPath(String path) {
        return path.endsWith("/auth/login")
                || path.endsWith("/auth/admin/login")
                || "/auth/register".equals(path)
                || "/user/auth/register".equals(path)
                || "/auth/email-code".equals(path)
                || "/user/auth/email-code".equals(path)
                || path.endsWith("/error")
                || path.endsWith("/swagger-ui.html")
                || path.contains("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.endsWith("/hi")
                || isPublicBusinessPath(path);
    }

    private boolean isPublicBusinessPath(String path) {
        return "/business/category/tree".equals(path)
                || "/business/product/list".equals(path)
                || "/business/product/suggestions".equals(path)
                || "/business/pay/alipay/notify".equals(path)
                || path.matches("^/business/product/[0-9]+$")
                || path.matches("^/business/product/[0-9]+/(reviews|review-summary|recommendations)$");
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private boolean validateToken(String token) {
        try {
            return JwtUtil.validate(token, secretKey);
        } catch (Exception exception) {
            log.error("令牌签名校验失败", exception);
            return false;
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
