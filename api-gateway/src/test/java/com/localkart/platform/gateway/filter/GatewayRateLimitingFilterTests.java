package com.localkart.platform.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRateLimitingFilterTests {

    private final GatewayRateLimitingFilter filter = new GatewayRateLimitingFilter();

    @Test
    void filter_WhenWithinRateLimit_ShouldPassThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = ex -> Mono.empty();

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void filter_WhenExceedingRateLimit_ShouldReturnTooManyRequests() {
        InetSocketAddress remoteAddress = new InetSocketAddress("192.168.1.50", 8080);
        WebFilterChain chain = ex -> Mono.empty();

        // Loop 100 times to consume all 100 capacity tokens
        for (int i = 0; i < 100; i++) {
            MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                    .remoteAddress(remoteAddress)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            Mono<Void> result = filter.filter(exchange, chain);
            StepVerifier.create(result).verifyComplete();
            assertThat(exchange.getResponse().getStatusCode()).isNull();
        }

        // 101st request should be rate-limited
        MockServerHttpRequest rateLimitedRequest = MockServerHttpRequest.get("/test")
                .remoteAddress(remoteAddress)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(rateLimitedRequest);
        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("5");
    }

    @Test
    void getOrder_ShouldReturnHighestPrecedencePlusTwo() {
        assertThat(filter.getOrder()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 2);
    }
}
