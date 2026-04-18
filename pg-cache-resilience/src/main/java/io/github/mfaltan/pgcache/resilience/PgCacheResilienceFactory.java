package io.github.mfaltan.pgcache.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PgCacheResilienceFactory implements CacheResilienceFactory {
    private final CircuitBreakerRegistry registry;
    private final String prefix;

    @Override
    public CacheResilience create(String cacheName) {

        var cbName = prefix + "-" + cacheName;
        var cb = registry.circuitBreaker(cbName);

        return new PgCacheResilience(cb);
    }
}
