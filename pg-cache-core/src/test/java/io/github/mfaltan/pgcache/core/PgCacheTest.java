package io.github.mfaltan.pgcache.core;

import io.github.mfaltan.pgcache.core.exception.PgCacheCallerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Type;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PgCacheTest {

    private static final String CACHE_NAME = "test-cache";
    private static final String SOME_KEY = "someKey";
    private static final String SOME_OTHER_KEY = "someOtherKey";
    private static final String SOME_VALUE = "someValue";
    private static final byte[] KEY_BYTES = "key1-normalized".getBytes();
    private static final byte[] OTHER_KEY_BYTES = "key2-normalized".getBytes();
    private static final byte[] VALUE_BYTES = "value1-serialized".getBytes();

    private PgCache cache;

    @Mock
    private ValueSerializer serializer;

    @Mock
    private Callable<String> loader;

    @Mock
    private KeyEntry someKey, someOtherKey;

    @Mock
    private Type type;

    @BeforeEach
    void setUp() {
        cache = PgCache.builder()
                       .name(CACHE_NAME)
                       .serializer(serializer)
                       .build();
    }

    @Test
    void should_return_name() {
        assertThat(cache.getName()).isEqualTo(CACHE_NAME);
    }

    @Test
    void should_return_native_cache() {
        //WHEN
        var actual = cache.getNativeCache();

        //THEN
        assertThat(actual).isInstanceOf(ConcurrentHashMap.class);
    }

    @Test
    void should_put_and_get_value() {
        // GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(someKey.type()).thenReturn(type);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(serializer.serialize(SOME_VALUE)).thenReturn(VALUE_BYTES);
        when(serializer.deserialize(VALUE_BYTES, type)).thenReturn(SOME_VALUE);

        // WHEN
        cache.put(someKey, SOME_VALUE);
        var actual = cache.get(someKey);

        // THEN
        assertThat(actual).isNotNull();
        assertThat(actual.get()).isEqualTo(SOME_VALUE);
    }

    @Test
    void should_return_null_when_missing() {
        //GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);

        //WHEN
        var actual = cache.get(someKey);

        //THEN
        assertThat(actual).isNull();
    }

    @Test
    void should_return_typed_null_when_missing() {
        //GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);

        //WHEN
        var actual = cache.get(someKey, String.class);

        //THEN
        assertThat(actual).isNull();
    }

    @Test
    void should_get_typed_value() {
        // GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(serializer.serialize(SOME_VALUE)).thenReturn(VALUE_BYTES);
        when(serializer.deserialize(VALUE_BYTES, String.class)).thenReturn(SOME_VALUE);


        // WHEN
        cache.put(someKey, SOME_VALUE);
        var actual = cache.get(someKey, String.class);

        // THEN
        assertThat(actual).isEqualTo(SOME_VALUE);
    }

    @Test
    void should_evict_value() {
        // GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(serializer.serialize(SOME_VALUE)).thenReturn(VALUE_BYTES);

        // WHEN
        cache.put(someKey, SOME_VALUE);
        cache.evict(someKey);
        var actual = cache.get(someKey);

        // THEN
        assertThat(actual).isNull();
    }

    @Test
    void should_clear_cache() {
        // GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(someOtherKey.rawKey()).thenReturn(SOME_OTHER_KEY);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(serializer.serialize(SOME_OTHER_KEY)).thenReturn(OTHER_KEY_BYTES);
        when(serializer.serialize(SOME_VALUE)).thenReturn(VALUE_BYTES);

        // WHEN
        cache.put(someKey, SOME_VALUE);
        cache.put(someOtherKey,SOME_VALUE);
        cache.clear();
        var actual1 = cache.get(someKey);
        var actual2 = cache.get(someOtherKey);

        // THEN
        assertThat(actual1).isNull();
        assertThat(actual2).isNull();
    }

    @Test
    void should_load_value_with_callable_and_cache_it() throws Exception {
        // GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(someKey.type()).thenReturn(type);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(serializer.serialize(SOME_VALUE)).thenReturn(VALUE_BYTES);
        when(serializer.deserialize(VALUE_BYTES, type)).thenReturn(SOME_VALUE);

        when(loader.call()).thenReturn(SOME_VALUE);

        // WHEN
        var first = cache.get(someKey, loader);
        var second = cache.get(someKey, loader);

        // THEN
        assertThat(first).isEqualTo(SOME_VALUE);
        assertThat(second).isEqualTo(SOME_VALUE);
        verify(loader).call();
    }

    @Test
    void should_not_load_value_with_callable_because_it_is_already_cached() {
        // GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(someKey.type()).thenReturn(type);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(serializer.serialize(SOME_VALUE)).thenReturn(VALUE_BYTES);
        when(serializer.deserialize(VALUE_BYTES, type)).thenReturn(SOME_VALUE);

        // WHEN
        cache.put(someKey, SOME_VALUE);
        var actual = cache.get(someKey, loader);

        // THEN
        assertThat(actual).isEqualTo(SOME_VALUE);
        verifyNoInteractions(loader);
    }

    @Test
    void should_wrap_exception_from_callable() throws Exception {
        // GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        var e = new RuntimeException();
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(loader.call()).thenThrow(e);

        // WHEN / THEN
        assertThatThrownBy(() -> cache.get(someKey, loader))
                .isInstanceOf(PgCacheCallerException.class)
                .hasCause(e);
    }
}