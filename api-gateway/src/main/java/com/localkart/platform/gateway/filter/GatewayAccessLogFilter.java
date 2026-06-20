package com.localkart.platform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GatewayAccessLogFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().pathWithinApplication().value();
        String method = request.getMethod().name();
        String correlationId = (String) exchange.getAttributes().get(GatewayCorrelationFilter.CORRELATION_ID_HEADER);

        log.info("[API Gateway] Incoming request: Method={} Path={} CorrelationID={}", method, path, correlationId);

        return chain.filter(exchange)
                .doOnSuccess(aVoid -> logAccess(exchange, method, path, correlationId, startTime))
                .doOnError(throwable -> log.error("[API Gateway] Request failed: Method={} Path={} CorrelationID={} Error={}",
                        method, path, correlationId, throwable.getMessage()));
    }

    private void logAccess(ServerWebExchange exchange, String method, String path, String correlationId, long startTime) {
        ServerHttpResponse response = exchange.getResponse();
        long duration = System.currentTimeMillis() - startTime;
        int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 200;

        log.info("[API Gateway] Access Log: Method={} Path={} Status={} Latency={}ms CorrelationID={}",
                method, path, statusCode, duration, correlationId);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1; // Run after correlation filter
    }
}
