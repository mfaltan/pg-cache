package io.github.mfaltan.pgcache.resilience;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoOpCacheResilienceTest {

    private static final String VALUE = "value";

    @Mock
    private Supplier<String> primarySupplier;

    @Mock
    private Supplier<String> fallbackSupplier;

    @Mock
    private Runnable primaryRunnable;

    @Mock
    private Runnable fallbackRunnable;

    @InjectMocks
    private NoOpCacheResilience resilience;

    @Test
    void should_execute_primary_supplier_successfully() {
        // GIVEN
        when(primarySupplier.get()).thenReturn(VALUE);

        // WHEN
        var result = resilience.execute(primarySupplier, fallbackSupplier);

        // THEN
        assertThat(result).isEqualTo(VALUE);
        verifyNoMoreInteractions(primarySupplier, fallbackSupplier, primaryRunnable, fallbackRunnable);
    }

    @Test
    void should_return_null_when_primary_supplier_throws_exception() {
        // GIVEN
        when(primarySupplier.get()).thenThrow(RuntimeException.class);

        // WHEN
        var result = resilience.execute(primarySupplier, fallbackSupplier);

        // THEN
        assertThat(result).isNull();
        verifyNoMoreInteractions(primarySupplier, fallbackSupplier, primaryRunnable, fallbackRunnable);
    }

    @Test
    void should_execute_primary_runnable_successfully() {
        // WHEN
        resilience.execute(primaryRunnable, fallbackRunnable);

        // THEN
        verify(primaryRunnable).run();
        verifyNoMoreInteractions(primarySupplier, fallbackSupplier, primaryRunnable, fallbackRunnable);
    }

    @Test
    void should_not_throw_when_primary_runnable_throws_exception() {
        // GIVEN
        doThrow(RuntimeException.class).when(primaryRunnable).run();

        // WHEN
        resilience.execute(primaryRunnable, fallbackRunnable);

        // THEN
        verify(primaryRunnable).run();
        verifyNoMoreInteractions(primarySupplier, fallbackSupplier, primaryRunnable, fallbackRunnable);
    }
}