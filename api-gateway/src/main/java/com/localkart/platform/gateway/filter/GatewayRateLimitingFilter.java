package com.localkart.platform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GatewayRateLimitingFilter implements WebFilter, Ordered {

    private final Map<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();
    private static final int BUCKET_CAPACITY = 100;
    private static final double REFILL_RATE_PER_SECOND = 2.0;
    private static final long EVICTION_THRESHOLD_NANOS = 10L * 60 * 1_000_000_000L; // 10 minutes

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        String ip = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";

        TokenBucket bucket = ipBuckets.computeIfAbsent(ip, k -> new TokenBucket(BUCKET_CAPACITY, REFILL_RATE_PER_SECOND));

        if (bucket.tryConsume()) {
            return chain.filter(exchange);
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Retry-After", "5");
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2; // Run after correlation and access log
    }

    /**
     * Evicts stale token buckets that haven't been accessed within the eviction threshold.
     * Runs every 5 minutes to prevent unbounded memory growth from unique IP addresses.
     */
    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    public void evictStaleBuckets() {
        long now = System.nanoTime();
        int before = ipBuckets.size();
        ipBuckets.entrySet().removeIf(entry -> (now - entry.getValue().getLastAccessTime()) > EVICTION_THRESHOLD_NANOS);
        int evicted = before - ipBuckets.size();
        if (evicted > 0) {
            log.info("Rate limiter eviction: removed {} stale IP bucket(s). Active buckets: {}", evicted, ipBuckets.size());
        }
    }

    private static class TokenBucket {
        private final int capacity;
        private final double refillRate;
        private double tokens;
        private long lastRefillTime;
        private volatile long lastAccessTime;

        public TokenBucket(int capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefillTime = System.nanoTime();
            this.lastAccessTime = System.nanoTime();
        }

        public synchronized boolean tryConsume() {
            refill();
            lastAccessTime = System.nanoTime();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedTime = (now - lastRefillTime) / 1e9;
            lastRefillTime = now;
            tokens = Math.min(capacity, tokens + (elapsedTime * refillRate));
        }
    }
}
