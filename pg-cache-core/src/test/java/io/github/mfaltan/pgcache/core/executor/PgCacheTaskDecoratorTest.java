package io.github.mfaltan.pgcache.core.executor;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;

class PgCacheTaskDecoratorTest {

    private static final String KEY = "key";
    private static final String VALUE = "value";

    private final PgCacheTaskDecorator decorator = new PgCacheTaskDecorator();

    @Test
    void should_decorate_if_context_was_there() {
        //GIVEN
        var contextMap = Map.of(KEY, VALUE);

        //WHEN + THEN
        try (MockedStatic<MDC> mockedMdc = Mockito.mockStatic(MDC.class)) {
            mockedMdc.when(MDC::getCopyOfContextMap).thenReturn(contextMap);
            mockedMdc.when(() -> MDC.get(KEY)).thenReturn(VALUE);

            var runnable = decorator.decorate(() -> assertThat(MDC.get(KEY)).isEqualTo(VALUE));
            runnable.run();

            mockedMdc.verify(() -> MDC.setContextMap(contextMap));
            mockedMdc.verify(MDC::clear);
        }
    }

    @Test
    void should_decorate_if_context_was_note_there() {
        //WHEN + THEN
        try (MockedStatic<MDC> mockedMdc = Mockito.mockStatic(MDC.class)) {
            mockedMdc.when(MDC::getCopyOfContextMap).thenReturn(null);
            mockedMdc.when(() -> MDC.get(KEY)).thenReturn(null);

            var runnable = decorator.decorate(() -> assertThat(MDC.get(KEY)).isEqualTo(null));
            runnable.run();

            mockedMdc.verify(MDC::clear, times(2));
        }
    }
}