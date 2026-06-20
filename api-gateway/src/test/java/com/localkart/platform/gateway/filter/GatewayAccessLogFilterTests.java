package com.localkart.platform.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAccessLogFilterTests {

    private final GatewayAccessLogFilter filter = new GatewayAccessLogFilter();

    @Test
    void filter_ShouldLogAndPassThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test-path")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(GatewayCorrelationFilter.CORRELATION_ID_HEADER, "test-corr-id-access");

        WebFilterChain chain = ex -> Mono.empty();

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void getOrder_ShouldReturnHighestPrecedencePlusOne() {
        assertThat(filter.getOrder()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1);
    }
}
