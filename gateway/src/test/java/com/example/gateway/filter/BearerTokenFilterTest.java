package com.example.gateway.filter;

import com.example.common.security.JwtClaims;
import com.example.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.test.util.ReflectionTestUtils;

class BearerTokenFilterTest {
    private static final String SECRET = "gateway-filter-test-secret-long-enough";

    private ReactiveTokenStateChecker tokenStateChecker;
    private BearerTokenFilter filter;

    @BeforeEach
    void setUp() {
        tokenStateChecker = mock(ReactiveTokenStateChecker.class);
        filter = new BearerTokenFilter(tokenStateChecker);
        ReflectionTestUtils.setField(filter, "secretKey", SECRET);
    }

    @Test
    void categoryTreePassesWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/business/category/tree").build()
        );
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.filter(exchange, current -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertTrue(chained.get());
        verifyNoInteractions(tokenStateChecker);
    }

    @Test
    void adminCategoryStillRequiresToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/business/admin/category/list").build()
        );
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.filter(exchange, current -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertFalse(chained.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void productListPassesWithoutToken() {
        assertPublicPath("/business/product/list");
    }

    @Test
    void numericProductDetailPassesWithoutToken() {
        assertPublicPath("/business/product/7");
    }

    @Test
    void exactProductReviewReadsPassWithoutToken() {
        assertPublicPath("/business/product/7/reviews");
        assertPublicPath("/business/product/7/review-summary");
    }

    @Test
    void productDiscoveryReadsPassWithoutToken() {
        assertPublicPath("/business/product/suggestions");
        assertPublicPath("/business/product/7/recommendations");
    }

    @Test
    void reviewWritesAndOtherProductSubpathsStillRequireToken() {
        assertUnauthorizedWithoutToken("/business/review");
        assertUnauthorizedWithoutToken("/business/admin/review/list");
        assertUnauthorizedWithoutToken("/business/product/7/reviews/export");
    }

    @Test
    void onlyExactAlipayNotificationPathIsPublic() {
        assertPublicPath("/business/pay/alipay/notify");
        assertUnauthorizedWithoutToken("/business/pay/alipay/create");
        assertUnauthorizedWithoutToken("/business/pay/alipay/query");
        assertUnauthorizedWithoutToken("/business/pay/alipay/notify/extra");
    }

    @Test
    void registerPassesWithoutToken() {
        assertPublicPath("/auth/register");
        assertPublicPath("/user/auth/register");
    }

    @Test
    void emailCodeOnlyPassesOnTheExactUserPaths() {
        assertPublicPath("/auth/email-code");
        assertPublicPath("/user/auth/email-code");
        assertUnauthorizedWithoutToken("/business/auth/email-code");
    }

    @Test
    void unrelatedServiceRegisterPathStillRequiresToken() {
        assertUnauthorizedWithoutToken("/business/auth/register");
    }

    @Test
    void logoutAndRefreshStillRequireToken() {
        assertUnauthorizedWithoutToken("/auth/logout");
        assertUnauthorizedWithoutToken("/user/auth/refresh");
    }

    @Test
    void adminProductStillRequiresToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/business/admin/product/list").build()
        );
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.filter(exchange, current -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertFalse(chained.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void revokedTokenIsRejectedBeforeForwarding() {
        String token = token(List.of("USER"), 3L);
        when(tokenStateChecker.isBlacklisted(token)).thenReturn(Mono.just(true));
        MockServerWebExchange exchange = exchange("/business/cart/list", token);
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.filter(exchange, current -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertFalse(chained.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void currentAdminTokenPreservesProtectedAccess() {
        String token = token(List.of("ADMIN"), 3L);
        when(tokenStateChecker.isBlacklisted(token)).thenReturn(Mono.just(false));
        MockServerWebExchange exchange = exchange("/business/admin/product/list", token);
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.filter(exchange, current -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertTrue(chained.get());
    }

    @Test
    void currentUserTokenCannotAccessAdminPath() {
        String token = token(List.of("USER"), 3L);
        when(tokenStateChecker.isBlacklisted(token)).thenReturn(Mono.just(false));
        MockServerWebExchange exchange = exchange("/business/admin/product/list", token);
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.filter(exchange, current -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertFalse(chained.get());
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void tokenStateBackendFailureFailsClosedWithServiceUnavailable() {
        String token = token(List.of("ADMIN"), 3L);
        when(tokenStateChecker.isBlacklisted(anyString()))
                .thenReturn(Mono.error(new IllegalStateException("redis down")));
        MockServerWebExchange exchange = exchange("/business/admin/product/list", token);
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.filter(exchange, current -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertFalse(chained.get());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    @Test
    void downstreamFailureIsNotReclassifiedAsTokenStateFailure() {
        String token = token(List.of("USER"), 3L);
        when(tokenStateChecker.isBlacklisted(token)).thenReturn(Mono.just(false));
        MockServerWebExchange exchange = exchange("/business/cart/list", token);
        IllegalStateException downstream = new IllegalStateException("downstream failed");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> filter.filter(exchange, current -> Mono.error(downstream)).block());

        assertSame(downstream, thrown);
    }

    private void assertPublicPath(String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(path).build()
        );
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.filter(exchange, current -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertTrue(chained.get());
    }

    private void assertUnauthorizedWithoutToken(String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post(path).build()
        );
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.filter(exchange, current -> {
            chained.set(true);
            return Mono.empty();
        }).block();

        assertFalse(chained.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    private MockServerWebExchange exchange(String path, String token) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path)
                .header("Authorization", "Bearer " + token)
                .build());
    }

    private String token(List<String> roles, Long version) {
        return JwtUtil.sign(Map.of(
                        JwtClaims.ID, 42L,
                        JwtClaims.JTI, "gateway-jti-" + version + roles.getFirst(),
                        JwtClaims.TOKEN_VERSION, version,
                        JwtClaims.ROLES, roles),
                SECRET,
                new Date(System.currentTimeMillis() - 1_000),
                new Date(System.currentTimeMillis() + 60_000));
    }
}
