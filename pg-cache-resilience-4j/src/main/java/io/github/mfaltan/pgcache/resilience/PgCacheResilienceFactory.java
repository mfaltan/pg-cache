package io.github.mfaltan.pgcache.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class PgCacheResilienceFactory implements CacheResilienceFactory {

    private final CircuitBreakerRegistry registry;
    private final String prefix;

    @Override
    public CacheResilience create(String cacheName) {

        var cbName = prefix + "-" + cacheName;
        var cb = registry.circuitBreaker(cbName);

        configureMonitoring(cb, cbName);

        log.info("Circuit breaker {} registered", cbName);
        return new PgCacheResilience(cb);
    }

    protected void configureMonitoring(CircuitBreaker cb, String cbName) {
        cb.getEventPublisher()
          .onStateTransition(event -> {
              var transition = event.getStateTransition();
              log.info("CircuitBreaker {} transitioned from {} to {}", cbName, transition.getFromState(), transition.getToState());
          });
    }
}
