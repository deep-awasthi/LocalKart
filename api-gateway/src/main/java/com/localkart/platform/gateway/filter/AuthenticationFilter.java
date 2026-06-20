package com.localkart.platform.gateway.filter;

import com.localkart.platform.gateway.validator.RouteValidator;
import com.localkart.platform.shared.security.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final JwtUtils jwtUtils;

    public AuthenticationFilter(RouteValidator validator, JwtUtils jwtUtils) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if (validator.isSecured.test(request)) {
                // Check if Authorization header is present
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    log.warn("Missing Authorization Header for request: {}", request.getURI());
                    return onError(exchange, "AUTH-401", "Missing authorization header", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    log.warn("Invalid Authorization Header format: {}", authHeader);
                    return onError(exchange, "AUTH-401", "Invalid authorization header format", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);
                try {
                    // Extract claims and validate token
                    Claims claims = jwtUtils.extractAllClaims(token);
                    String username = claims.getSubject();
                    String roles = claims.get("roles", String.class);

                    if (jwtUtils.isTokenExpired(token)) {
                        log.warn("Token expired for user: {}", username);
                        return onError(exchange, "AUTH-401", "Token has expired", HttpStatus.UNAUTHORIZED);
                    }

                    // Mutate request headers to forward username and roles downstream
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-Auth-User", username)
                            .header("X-Auth-Roles", roles != null ? roles : "")
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());

                } catch (Exception e) {
                    log.error("Token validation failed: {}", e.getMessage());
                    return onError(exchange, "AUTH-401", "Invalid or tampered token", HttpStatus.UNAUTHORIZED);
                }
            }
            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String errorCode, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String errorBody = String.format("{\"success\":false,\"message\":\"%s\",\"errorCode\":\"%s\",\"timestamp\":%d}",
                message, errorCode, System.currentTimeMillis());

        DataBuffer buffer = response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Configuration fields if needed
    }
}
