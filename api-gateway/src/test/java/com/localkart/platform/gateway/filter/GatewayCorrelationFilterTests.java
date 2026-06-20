package com.localkart.platform.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayCorrelationFilterTests {

    private final GatewayCorrelationFilter filter = new GatewayCorrelationFilter();

    @Test
    void filter_WhenCorrelationIdHeaderPresent_ShouldPropagateIt() {
        String existingCorrelationId = "test-correlation-123";
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header(GatewayCorrelationFilter.CORRELATION_ID_HEADER, existingCorrelationId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = ex -> {
            ServerHttpRequest mutatedRequest = ex.getRequest();
            assertThat(mutatedRequest.getHeaders().getFirst(GatewayCorrelationFilter.CORRELATION_ID_HEADER))
                    .isEqualTo(existingCorrelationId);
            return Mono.empty();
        };

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(GatewayCorrelationFilter.CORRELATION_ID_HEADER))
                .isEqualTo(existingCorrelationId);

        String attributeCorrelationId = exchange.getAttribute(GatewayCorrelationFilter.CORRELATION_ID_HEADER);
        assertThat(attributeCorrelationId)
                .isEqualTo(existingCorrelationId);
    }

    @Test
    void filter_WhenCorrelationIdHeaderAbsent_ShouldGenerateAndPropagateIt() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        WebFilterChain chain = ex -> {
            ServerHttpRequest mutatedRequest = ex.getRequest();
            String correlationId = mutatedRequest.getHeaders().getFirst(GatewayCorrelationFilter.CORRELATION_ID_HEADER);
            assertThat(correlationId).isNotNull().isNotBlank();
            return Mono.empty();
        };

        Mono<Void> result = filter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();

        String responseCorrelationId = exchange.getResponse().getHeaders().getFirst(GatewayCorrelationFilter.CORRELATION_ID_HEADER);
        assertThat(responseCorrelationId).isNotNull().isNotBlank();

        String attributeCorrelationId = exchange.getAttribute(GatewayCorrelationFilter.CORRELATION_ID_HEADER);
        assertThat(attributeCorrelationId).isEqualTo(responseCorrelationId);
    }

    @Test
    void getOrder_ShouldReturnHighestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
    }
}
