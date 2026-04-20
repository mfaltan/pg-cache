package io.github.mfaltan.pgcache.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgCacheResilienceTest {

    private static final String VALUE = "value";

    @InjectMocks
    private PgCacheResilience resilience;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private Supplier<String> primary;

    @Mock
    private Supplier<String> fallback;

    @Mock
    private Supplier<String> decoratedSupplier;

    @Mock
    private Runnable primaryRunnable;

    @Mock
    private Runnable fallbackRunnable;

    @Mock
    private Runnable decoratedRunnable;

    @Test
    void should_return_primary_result_when_no_exception() {
        // GIVEN
        try (var mocked = mockStatic(CircuitBreaker.class)) {
            mocked.when(() -> CircuitBreaker.decorateSupplier(circuitBreaker, primary)).thenReturn(decoratedSupplier);

            when(decoratedSupplier.get()).thenReturn(VALUE);

            // WHEN
            var result = resilience.execute(primary, fallback);

            // THEN
            assertThat(result).isEqualTo(VALUE);
            verify(decoratedSupplier).get();
            verifyNoMoreInteractions(circuitBreaker, primary, fallback, decoratedSupplier, primaryRunnable, fallbackRunnable, decoratedRunnable);
        }
    }

    @Test
    void should_use_fallback_when_call_not_permitted_exception() {
        // GIVEN
        try (var mocked = mockStatic(CircuitBreaker.class)) {
            mocked.when(() -> CircuitBreaker.decorateSupplier(circuitBreaker, primary)).thenReturn(decoratedSupplier);

            when(decoratedSupplier.get()).thenThrow(CallNotPermittedException.class);
            when(fallback.get()).thenReturn(VALUE);

            // WHEN
            var result = resilience.execute(primary, fallback);

            // THEN
            assertThat(result).isEqualTo(VALUE);
            verify(decoratedSupplier).get();
            verify(fallback).get();
            verifyNoMoreInteractions(circuitBreaker, primary, fallback, decoratedSupplier, primaryRunnable, fallbackRunnable, decoratedRunnable);
        }
    }

    @Test
    void should_use_fallback_when_generic_exception() {
        // GIVEN
        try (var mocked = mockStatic(CircuitBreaker.class)) {
            mocked.when(() -> CircuitBreaker.decorateSupplier(circuitBreaker, primary)).thenReturn(decoratedSupplier);

            when(decoratedSupplier.get()).thenThrow(RuntimeException.class);
            when(fallback.get()).thenReturn(VALUE);

            // WHEN
            var result = resilience.execute(primary, fallback);

            // THEN
            assertThat(result).isEqualTo(VALUE);
            verify(decoratedSupplier).get();
            verify(fallback).get();
            verifyNoMoreInteractions(circuitBreaker, primary, fallback, decoratedSupplier, primaryRunnable, fallbackRunnable, decoratedRunnable);
        }
    }

    @Test
    void should_execute_primary_runnable_when_no_exception() {
        // GIVEN
        try (var mocked = mockStatic(CircuitBreaker.class)) {
            mocked.when(() -> CircuitBreaker.decorateRunnable(circuitBreaker, primaryRunnable)).thenReturn(decoratedRunnable);

            // WHEN
            resilience.execute(primaryRunnable, fallbackRunnable);

            // THEN
            verify(decoratedRunnable).run();
            verifyNoMoreInteractions(circuitBreaker, primary, fallback, decoratedSupplier, primaryRunnable, fallbackRunnable, decoratedRunnable);
        }
    }

    @Test
    void should_run_fallback_when_call_not_permitted_exception_runnable() {
        // GIVEN
        try (var mocked = mockStatic(CircuitBreaker.class)) {
            mocked.when(() -> CircuitBreaker.decorateRunnable(circuitBreaker, primaryRunnable)).thenReturn(decoratedRunnable);

            doThrow(CallNotPermittedException.class).when(decoratedRunnable).run();

            // WHEN
            resilience.execute(primaryRunnable, fallbackRunnable);

            // THEN
            verify(decoratedRunnable).run();
            verify(fallbackRunnable).run();
            verifyNoMoreInteractions(circuitBreaker, primary, fallback, decoratedSupplier, primaryRunnable, fallbackRunnable, decoratedRunnable);
        }
    }

    @Test
    void should_run_fallback_when_generic_exception_runnable() {
        // GIVEN
        try (var mocked = mockStatic(CircuitBreaker.class)) {
            mocked.when(() -> CircuitBreaker.decorateRunnable(circuitBreaker, primaryRunnable)).thenReturn(decoratedRunnable);

            doThrow(RuntimeException.class).when(decoratedRunnable).run();

            // WHEN
            resilience.execute(primaryRunnable, fallbackRunnable);

            // THEN
            verify(decoratedRunnable).run();
            verify(fallbackRunnable).run();
            verifyNoMoreInteractions(circuitBreaker, primary, fallback, decoratedSupplier, primaryRunnable, fallbackRunnable, decoratedRunnable);
        }
    }
}