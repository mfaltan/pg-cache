package io.github.mfaltan.pgcache.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgCacheResilienceFactoryTest {

    private static final String PREFIX = "prefix-";

    private PgCacheResilienceFactory factory;

    @Mock
    private CircuitBreakerRegistry registry;

    @Mock
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void init() {
        factory = new PgCacheResilienceFactory(registry, PREFIX);
    }

    @Test
    void should_create_pg_cache_resilience_with_correct_circuit_breaker() {

        // GIVEN
        var cache = "cache";
        var cbName = PREFIX + "-" + cache;

        var spiedFactory = Mockito.spy(factory);
        doNothing().when(spiedFactory).configureMonitoring(circuitBreaker, cbName);
        when(registry.circuitBreaker(cbName)).thenReturn(circuitBreaker);

        // WHEN
        var result = spiedFactory.create(cache);

        // THEN
        assertThat(result).isInstanceOf(PgCacheResilience.class);
        verify(registry).circuitBreaker(cbName);
        verifyNoMoreInteractions(registry, circuitBreaker);
    }
}