package com.example.gateway.filter;

import reactor.core.publisher.Mono;

public interface ReactiveTokenStateChecker {
    Mono<Boolean> isBlacklisted(String token);
}
