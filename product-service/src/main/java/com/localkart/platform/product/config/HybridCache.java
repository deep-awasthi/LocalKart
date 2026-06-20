package com.localkart.platform.product.config;

import org.springframework.cache.Cache;
import java.util.concurrent.Callable;

public class HybridCache implements Cache {

    private final String name;
    private final Cache l1Cache; // Caffeine Local Cache
    private final Cache l2Cache; // Redis Distributed Cache

    public HybridCache(String name, Cache l1Cache, Cache l2Cache) {
        this.name = name;
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        // 1. Try reading from L1 (Caffeine)
        ValueWrapper wrapper = l1Cache.get(key);
        if (wrapper != null) {
            return wrapper;
        }

        // 2. Try reading from L2 (Redis)
        wrapper = l2Cache.get(key);
        if (wrapper != null) {
            // Cache back to L1 for subsequent requests
            l1Cache.put(key, wrapper.get());
            return wrapper;
        }

        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = l1Cache.get(key, type);
        if (value != null) {
            return value;
        }

        value = l2Cache.get(key, type);
        if (value != null) {
            l1Cache.put(key, value);
            return value;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }

        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        l1Cache.put(key, value);
        l2Cache.put(key, value);
    }

    @Override
    public void evict(Object key) {
        l1Cache.evict(key);
        l2Cache.evict(key);
    }

    @Override
    public void clear() {
        l1Cache.clear();
        l2Cache.clear();
    }
}
