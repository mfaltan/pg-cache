package io.github.mfaltan.pgcache.resilience;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class PgCacheResilience implements CacheResilience {

    private final CircuitBreaker circuitBreaker;

    @Override
    public <T> T execute(Supplier<T> primary, Supplier<T> fallback) {
        var decorated = CircuitBreaker.decorateSupplier(circuitBreaker, primary);

        try {
            return decorated.get();
        } catch (CallNotPermittedException e) {
            return fallback.get();
        } catch (Exception e) {
            // real failure → log warning
            log.warn(Constants.MARKER, "Cache backend failure, caught by circuit breaker. Executing fallback method.", e);
            return fallback.get();
        }
    }

    @Override
    public void execute(Runnable primary, Runnable fallback) {
        var decorated = CircuitBreaker.decorateRunnable(circuitBreaker, primary);

        try {
            decorated.run();
        } catch (CallNotPermittedException e) {
            fallback.run();
        } catch (Exception e) {
            // real failure → log warning
            log.warn(Constants.MARKER, "Cache backend failure, caught by circuit breaker. Executing fallback method.", e);
            fallback.run();
        }
    }
}