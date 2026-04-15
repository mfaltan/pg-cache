package io.github.mfaltan.pgcache.core;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import io.github.mfaltan.pgcache.core.exception.PgCacheCallerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Type;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PgCacheTest {

    private static final Long SOME_LONG_KEY = 1L;
    private static final String CACHE_NAME = "test-cache";
    private static final String SOME_KEY = "someKey";
    private static final String SOME_VALUE = "someValue";
    private static final byte[] KEY_BYTES = "key1-normalized".getBytes();
    private static final byte[] VALUE_BYTES = "value1-serialized".getBytes();

    private PgCache cache;

    @Mock
    private ValueSerializer serializer;

    @Mock
    private Store store;

    @Mock
    private Callable<String> loader;

    @Mock
    private KeyEntry someKey;

    @Mock
    private Type type;

    @Mock
    private HashFunction hashFunction;

    @Mock
    private HashCode hashCode;

    @Mock
    private CacheEntry cacheEntry;

    @BeforeEach
    void setUp() {
        cache = PgCache.builder()
                       .name(CACHE_NAME)
                       .serializer(serializer)
                       .store(store)
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
        assertThat(actual).isEqualTo(store);
    }

    @Test
    void should_successfully_get_value() {
        // GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(serializer.deserialize(VALUE_BYTES, type)).thenReturn(SOME_VALUE);
        when(store.get(SOME_LONG_KEY)).thenReturn(cacheEntry);
        when(cacheEntry.value()).thenReturn(VALUE_BYTES);
        when(cacheEntry.normalizedKey()).thenReturn(KEY_BYTES);
        when(someKey.type()).thenReturn(type);

        try (MockedStatic<Hashing> hashing = mockStatic(Hashing.class)) {

            hashing.when(Hashing::murmur3_128).thenReturn(hashFunction);
            when(hashFunction.hashBytes(KEY_BYTES)).thenReturn(hashCode);
            when(hashCode.asLong()).thenReturn(SOME_LONG_KEY);

            // WHEN
            var actual = cache.get(someKey);

            // THEN
            assertThat(actual).isNotNull();
            assertThat(actual.get()).isEqualTo(SOME_VALUE);
        }
    }

    @Test
    void should_return_null_when_missing() {
        //GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(store.get(SOME_LONG_KEY)).thenReturn(null);

        try (MockedStatic<Hashing> hashing = mockStatic(Hashing.class)) {

            hashing.when(Hashing::murmur3_128).thenReturn(hashFunction);
            when(hashFunction.hashBytes(KEY_BYTES)).thenReturn(hashCode);
            when(hashCode.asLong()).thenReturn(SOME_LONG_KEY);

            // WHEN
            var actual = cache.get(someKey);

            // THEN
            assertThat(actual).isNull();
        }
    }

    @Test
    void should_return_typed_null_when_missing() {
        //GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(store.get(SOME_LONG_KEY)).thenReturn(null);

        try (MockedStatic<Hashing> hashing = mockStatic(Hashing.class)) {

            hashing.when(Hashing::murmur3_128).thenReturn(hashFunction);
            when(hashFunction.hashBytes(KEY_BYTES)).thenReturn(hashCode);
            when(hashCode.asLong()).thenReturn(SOME_LONG_KEY);

            // WHEN
            var actual = cache.get(someKey, String.class);

            // THEN
            assertThat(actual).isNull();
        }
    }

    @Test
    void should_get_typed_value() {
        // GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(serializer.deserialize(VALUE_BYTES, String.class)).thenReturn(SOME_VALUE);
        when(store.get(SOME_LONG_KEY)).thenReturn(cacheEntry);
        when(cacheEntry.value()).thenReturn(VALUE_BYTES);
        when(cacheEntry.normalizedKey()).thenReturn(KEY_BYTES);

        try (MockedStatic<Hashing> hashing = mockStatic(Hashing.class)) {

            hashing.when(Hashing::murmur3_128).thenReturn(hashFunction);
            when(hashFunction.hashBytes(KEY_BYTES)).thenReturn(hashCode);
            when(hashCode.asLong()).thenReturn(SOME_LONG_KEY);

            // WHEN
            var actual = cache.get(someKey, String.class);

            // THEN
            assertThat(actual).isEqualTo(SOME_VALUE);
        }
    }

    @Test
    void should_evict_value() {
        // GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);

        try (MockedStatic<Hashing> hashing = mockStatic(Hashing.class)) {

            hashing.when(Hashing::murmur3_128).thenReturn(hashFunction);
            when(hashFunction.hashBytes(KEY_BYTES)).thenReturn(hashCode);
            when(hashCode.asLong()).thenReturn(SOME_LONG_KEY);

            // WHEN
            cache.evict(someKey);

            // THEN
            verify(store).remove(SOME_LONG_KEY);
        }
    }

    @Test
    void should_clear_cache() {
        // WHEN
        cache.clear();

        // THEN
        verify(store).clear();
    }

    @Test
    void should_load_value_with_callable_and_cache_it() throws Exception {
        // GIVEN
        var entry = createCacheEntry(KEY_BYTES, VALUE_BYTES);
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(serializer.serialize(SOME_VALUE)).thenReturn(VALUE_BYTES);

        when(loader.call()).thenReturn(SOME_VALUE);

        try (MockedStatic<Hashing> hashing = mockStatic(Hashing.class)) {

            hashing.when(Hashing::murmur3_128).thenReturn(hashFunction);
            when(hashFunction.hashBytes(KEY_BYTES)).thenReturn(hashCode);
            when(hashCode.asLong()).thenReturn(SOME_LONG_KEY);

            // WHEN
            var first = cache.get(someKey, loader);

            // THEN
            assertThat(first).isEqualTo(SOME_VALUE);
            verify(loader).call();
            verify(store).put(SOME_LONG_KEY, entry);
        }
    }

    @Test
    void should_not_load_value_with_callable_because_it_is_already_cached() {
        // GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(store.get(SOME_LONG_KEY)).thenReturn(cacheEntry);
        when(someKey.type()).thenReturn(type);
        when(cacheEntry.value()).thenReturn(VALUE_BYTES);
        when(cacheEntry.normalizedKey()).thenReturn(KEY_BYTES);
        when(serializer.deserialize(VALUE_BYTES, type)).thenReturn(SOME_VALUE);
        try (MockedStatic<Hashing> hashing = mockStatic(Hashing.class)) {

            hashing.when(Hashing::murmur3_128).thenReturn(hashFunction);
            when(hashFunction.hashBytes(KEY_BYTES)).thenReturn(hashCode);
            when(hashCode.asLong()).thenReturn(SOME_LONG_KEY);

            // WHEN
            var actual = cache.get(someKey, loader);

            // THEN
            assertThat(actual).isEqualTo(SOME_VALUE);
            verifyNoInteractions(loader);
            verify(store, times(0)).put(anyLong(), any(CacheEntry.class));
        }
    }

    @Test
    void should_wrap_exception_from_callable() throws Exception {
        // GIVEN
        when(someKey.rawKey()).thenReturn(SOME_KEY);
        var e = new RuntimeException();
        when(serializer.serialize(SOME_KEY)).thenReturn(KEY_BYTES);
        when(store.get(SOME_LONG_KEY)).thenReturn(null);
        when(loader.call()).thenThrow(e);

        try (MockedStatic<Hashing> hashing = mockStatic(Hashing.class)) {

            hashing.when(Hashing::murmur3_128).thenReturn(hashFunction);
            when(hashFunction.hashBytes(KEY_BYTES)).thenReturn(hashCode);
            when(hashCode.asLong()).thenReturn(SOME_LONG_KEY);

            // WHEN + THEN
            assertThatThrownBy(() -> cache.get(someKey, loader))
                    .isInstanceOf(PgCacheCallerException.class)
                    .hasCause(e);
        }
    }

    @Test
    void should_throw_exception_because_of_incorrect_provided_key() {
        // WHEN + THEN
        assertThatThrownBy(()->cache.get("wrongKey")).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_evict_expired_1000() {
        // GIVEN
        var limit = 1000;

        // WHEN
        cache.evictExpired(limit);

        // THEN
        verify(store).evictExpired(limit);
    }

    private static CacheEntry createCacheEntry(byte[] normalizedKey, byte[] serializedValue) {
        return CacheEntry.builder()
                         .normalizedKey(normalizedKey)
                         .value(serializedValue)
                         .build();

    }
}