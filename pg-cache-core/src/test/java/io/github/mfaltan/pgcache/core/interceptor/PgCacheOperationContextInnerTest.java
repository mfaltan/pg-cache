package io.github.mfaltan.pgcache.core.interceptor;

import io.github.mfaltan.pgcache.core.domain.KeyEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgCacheOperationContextInnerTest {

    private static final Object O1 = "o1";
    private static final Object O2 = "o2";

    @Mock
    private Function<Object, Object> keyFunction;
    @Mock
    private Supplier<Object> keySupplier;
    @Mock
    private Supplier<Method> methodSupplier;
    @Mock
    private Method method;
    @Mock
    private Type type;

    @AfterEach
    void verifyNoUnexpectedInteractions() {
        Mockito.verifyNoMoreInteractions(
                keyFunction,
                keySupplier,
                methodSupplier,
                method,
                type);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void should_generate_key(boolean typeNeeded) {
        when(keyFunction.apply(O1)).thenReturn(O2);
        test(typeNeeded, false, inner -> inner.generateKey(O1));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void should_generate_null_key(boolean typeNeeded) {
        when(keyFunction.apply(O1)).thenReturn(null);
        test(typeNeeded, true, inner -> inner.generateKey(O1));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void should_get_generated_key(boolean typeNeeded) {
        when(keySupplier.get()).thenReturn(O2);
        test(typeNeeded, false, PgCacheOperationContextInner::getGeneratedKey);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void should_get_generated_null_key(boolean typeNeeded) {
        when(keySupplier.get()).thenReturn(null);
        test(typeNeeded, true, PgCacheOperationContextInner::getGeneratedKey);
    }

    private void test(boolean typeNeeded, boolean expectNull, Function<PgCacheOperationContextInner, Object> doFunction) {
        //GIVEN
        if (typeNeeded && !expectNull) {
            when(methodSupplier.get()).thenReturn(method);
            when(method.getGenericReturnType()).thenReturn(type);
        }

        var expected = expectNull ? null : typeNeeded ? KeyEntry.builder()
                                                                .type(type)
                                                                .rawKey(O2)
                                                                .build() : O2;
        //WHEN
        var inner = create(typeNeeded);
        var actual = doFunction.apply(inner);

        //THEN
        assertThat(actual).isEqualTo(expected);
    }

    private PgCacheOperationContextInner create(boolean typeNeeded) {
        return PgCacheOperationContextInner.builder()
                                           .keyFunction(keyFunction)
                                           .keySupplier(keySupplier)
                                           .methodSupplier(methodSupplier)
                                           .typeNeeded(typeNeeded)
                                           .build();
    }


}