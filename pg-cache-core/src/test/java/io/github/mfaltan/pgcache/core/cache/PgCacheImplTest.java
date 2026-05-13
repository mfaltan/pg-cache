package io.github.mfaltan.pgcache.core.cache;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.common.PgCacheProperties.CacheProperties;
import io.github.mfaltan.pgcache.core.cache.PgCacheNoOp.Type;
import io.github.mfaltan.pgcache.core.domain.CacheEntry;
import io.github.mfaltan.pgcache.core.domain.KeyEntry;
import io.github.mfaltan.pgcache.core.exception.PgCacheKeyException;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.PgCacheGeneralSerializer;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializer;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerPair;
import io.github.mfaltan.pgcache.core.store.PgCacheStore;
import io.github.mfaltan.pgcache.resilience.CacheResilience;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgCacheImplTest {

    private static final Long KEY_LONG = 221L;
    private static final String CACHE_NAME = "test-cache";
    private static final String KEY = "key";
    private static final byte[] KEY_BYTES = KEY.getBytes();
    private static final String VALUE_1 = "value1";
    private static final byte[] VALUE_1_BYTES = VALUE_1.getBytes();
    private static final Integer TTL_SECONDS = 1;
    private static final Integer ASYNC_GET_TIMEOUT = 2;
    private static final Integer ASYNC_GET_LOADER_TIMEOUT = 3;
    private static final Class<String> CLASS_TYPE = String.class;

    private PgCacheImpl cache;

    @Mock
    private PgCacheStore store;
    @Mock
    private CacheExecutorHolder executorHolder;
    @Mock
    private CacheResilience cacheResilience;
    @Mock
    private PgCacheGeneralSerializer generalSerializer;
    @Mock
    private PgCacheSerializer keySerializer, valueSerializer;
    @Mock
    private PgCacheSerializerPair serializerPair;
    @Mock
    private PgCacheProperties properties;
    @Mock
    private CacheProperties cacheProperties;

    @Mock
    private KeyEntry keyEntry;
    @Mock
    private PgCacheNoOp cacheNoOp;
    @Mock
    private ValueWrapper vw1, vw2;
    @Mock
    private Callable<String> valueLoader;
    @Mock
    private Executor executor;
    @Mock
    private Object rawKey;
    @Mock
    private HashFunction hashFunction;
    @Mock
    private HashCode hashCode;
    @Mock
    private CacheEntry cacheEntry;

    @BeforeEach
    void init() {
        when(properties.getCaches()).thenReturn(Map.of(CACHE_NAME, cacheProperties));
        when(cacheProperties.getTtlSeconds()).thenReturn(null);
        when(properties.getDefaultTtlSeconds()).thenReturn(TTL_SECONDS);
        when(properties.getAsyncGetTimeout()).thenReturn(ASYNC_GET_TIMEOUT);
        when(properties.getAsyncGetWithLoaderTimeout()).thenReturn(ASYNC_GET_LOADER_TIMEOUT);

        cache = new PgCacheImpl(CACHE_NAME, store, executorHolder, cacheResilience, generalSerializer, serializerPair, properties);
    }

    @AfterEach
    void verifyNoUnexpectedInteractions() {
        Mockito.verifyNoMoreInteractions(
                store,
                executorHolder,
                cacheResilience,
                generalSerializer,
                keySerializer, valueSerializer,
                properties,
                cacheProperties,
                keyEntry,
                cacheNoOp,
                vw1, vw2,
                valueLoader,
                executor,
                rawKey,
                serializerPair,
                hashFunction,
                hashCode,
                cacheEntry
        );
    }

    @Test
    void should_return_name() {
        //WHEN
        var actual = cache.getName();

        //THEN
        assertThat(actual).isEqualTo(CACHE_NAME);
    }

    @Test
    void should_return_self() {
        //WHEN
        var actual = cache.getNativeCache();

        //THEN
        assertThat(actual).isEqualTo(store);
    }

    @Test
    void should_return_value_wrapper() {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doReturn(keyEntry).when(spiedCache).keyToKeyEntry(KEY);
        doReturn(cacheNoOp).when(spiedCache).getCacheNoOp();
        doReturn(vw1).when(spiedCache).getInternal(keyEntry);

        stubCacheResilienceGet();

        //WHEN
        var actual = spiedCache.get(KEY);

        //THEN
        assertThat(actual).isEqualTo(vw1);
        verify(cacheNoOp).get(KEY);
    }

    @Test
    void should_return_value_when_type_provided() {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doReturn(keyEntry).when(spiedCache).keyToKeyEntry(KEY);
        doReturn(cacheNoOp).when(spiedCache).getCacheNoOp();
        doReturn(VALUE_1).when(spiedCache).getInternal(keyEntry, CLASS_TYPE);

        stubCacheResilienceGet();

        //WHEN
        var actual = spiedCache.get(KEY, CLASS_TYPE);

        //THEN
        assertThat(actual).isEqualTo(VALUE_1);
        verify(cacheNoOp).get(KEY, CLASS_TYPE);
    }

    @Test
    void should_return_value_and_not_call_loader() {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doReturn(vw1).when(spiedCache).get(KEY);
        when(vw1.get()).thenReturn(VALUE_1);

        //WHEN
        var actual = spiedCache.get(KEY, valueLoader);

        //THEN
        assertThat(actual).isEqualTo(VALUE_1);
    }

    @Test
    void should_call_loader_and_return_value() throws Exception {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doReturn(null).when(spiedCache).get(KEY);
        when(valueLoader.call()).thenReturn(VALUE_1);
        doNothing().when(spiedCache).put(KEY, VALUE_1);

        //WHEN
        var actual = spiedCache.get(KEY, valueLoader);

        //THEN
        assertThat(actual).isEqualTo(VALUE_1);
    }

    @Test
    void should_call_loader_and_fail() throws Exception {
        //GIVEN
        var e = new RuntimeException();
        var spiedCache = Mockito.spy(cache);
        doReturn(null).when(spiedCache).get(KEY);
        when(valueLoader.call()).thenThrow(e);

        //WHEN + THEN
        assertThatThrownBy(() -> spiedCache.get(KEY, valueLoader)).isInstanceOf(Cache.ValueRetrievalException.class)
                                                                  .hasCause(e);
    }

    @Test
    void should_put_value() {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doReturn(keyEntry).when(spiedCache).keyToKeyEntry(KEY);
        doReturn(cacheNoOp).when(spiedCache).getCacheNoOp();
        doNothing().when(spiedCache).putInternal(keyEntry, VALUE_1);
        when(executorHolder.getWriteExecutor()).thenReturn(executor);

        stubCacheResilienceRun();
        stubExecutor();

        //WHEN
        spiedCache.put(KEY, VALUE_1);

        //THEN
        verify(cacheNoOp).put(KEY, VALUE_1);
    }

    @Test
    void should_evict_key() {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doReturn(keyEntry).when(spiedCache).keyToKeyEntry(KEY);
        doReturn(cacheNoOp).when(spiedCache).getCacheNoOp();
        doNothing().when(spiedCache).evictInternal(keyEntry);
        when(executorHolder.getWriteExecutor()).thenReturn(executor);

        stubCacheResilienceRun();
        stubExecutor();

        //WHEN
        spiedCache.evict(KEY);

        //THEN
        verify(cacheNoOp).evict(KEY);
    }

    @Test
    void should_clear() {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doReturn(cacheNoOp).when(spiedCache).getCacheNoOp();
        when(executorHolder.getClearExecutor()).thenReturn(executor);

        stubCacheResilienceRun();
        stubExecutor();

        //WHEN
        spiedCache.clear();

        //THEN
        verify(cacheNoOp).clear();
        verify(store).clear(CACHE_NAME);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_retrieve() throws ExecutionException, InterruptedException {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doAnswer(invocation -> {
            var key = invocation.getArgument(0);
            Assertions.assertThat(key).isEqualTo(KEY);
            Callable<?> callable = invocation.getArgument(1);
            return callable.call();
        }).when(spiedCache).get(any(Object.class), any(Callable.class));

        //WHEN
        CompletableFuture<?> completableFuture = spiedCache.retrieve(KEY, () -> CompletableFuture.supplyAsync(() -> vw1));

        //THEN
        assertThat(completableFuture).isNotNull();
        assertThat(completableFuture.get()).isEqualTo(vw1);
    }

    @Test
    void should_retrieve_with_loader() throws ExecutionException, InterruptedException {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doReturn(vw1).when(spiedCache).get(KEY);

        //WHEN
        CompletableFuture<?> completableFuture = spiedCache.retrieve(KEY);

        //THEN
        assertThat(completableFuture).isNotNull();
        assertThat(completableFuture.get()).isEqualTo(vw1);
    }

    @Test
    void should_evict_expired() {
        //GIVEN
        int limit = 22;

        var spiedCache = Mockito.spy(cache);
        doReturn(cacheNoOp).when(spiedCache).getCacheNoOp();
        when(executorHolder.getWriteExecutor()).thenReturn(executor);

        stubCacheResilienceRun();
        stubExecutor();

        //WHEN
        spiedCache.evictExpired(limit);

        //THEN
        verify(cacheNoOp).evictExpired(limit);
        verify(store).evictExpired(limit, CACHE_NAME);
    }

    @Test
    void should_return_pg_cache_no_op() {
        //GIVEN
        var expected = new PgCacheNoOp(CACHE_NAME, Type.TEMPORARILY);

        //WHEN
        var actual = cache.getCacheNoOp();

        //THEN
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_correct_cached_value_internal() {
        //GIVEN
        when(keyEntry.rawKey()).thenReturn(rawKey);
        doReturn(keySerializer).when(serializerPair).keySerializer();
        when(keySerializer.serializeValue(rawKey)).thenReturn(KEY_BYTES);

        //WHEN
        try (MockedStatic<Hashing> hashing = mockStatic(Hashing.class)) {
            hashing.when(Hashing::murmur3_128).thenReturn(hashFunction);
            when(hashFunction.hashBytes(KEY_BYTES)).thenReturn(hashCode);
            when(hashCode.asLong()).thenReturn(KEY_LONG);
            when(store.get(KEY_LONG, CACHE_NAME)).thenReturn(cacheEntry);
            when(cacheEntry.value()).thenReturn(KEY_BYTES);
            when(keyEntry.type()).thenReturn(String.class);
            doReturn(valueSerializer).when(serializerPair).valueSerializer();
            when(valueSerializer.deserialize(KEY_BYTES)).thenReturn(VALUE_1);
            when(cacheEntry.normalizedKey()).thenReturn(KEY_BYTES);

            var actual = cache.getInternal(keyEntry);

            //THEN returns wrapper with correct value
            assertThat(actual).isNotNull();
            assertThat(actual.get()).isEqualTo(VALUE_1);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_correct_null_internal() {
        //GIVEN
        when(keyEntry.rawKey()).thenReturn(rawKey);
        doReturn(keySerializer).when(serializerPair).keySerializer();
        when(keySerializer.serializeValue(rawKey)).thenReturn(KEY_BYTES);

        //WHEN
        try (MockedStatic<Hashing> hashing = mockStatic(Hashing.class)) {
            hashing.when(Hashing::murmur3_128).thenReturn(hashFunction);
            when(hashFunction.hashBytes(KEY_BYTES)).thenReturn(hashCode);
            when(hashCode.asLong()).thenReturn(KEY_LONG);
            when(store.get(KEY_LONG, CACHE_NAME)).thenReturn(null);

            var actual = cache.getInternal(keyEntry);

            //THEN return null wrapper, because nothing was cached
            assertThat(actual).isNull();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_correct_cached_null_internal() {
        //GIVEN
        when(keyEntry.rawKey()).thenReturn(rawKey);
        doReturn(keySerializer).when(serializerPair).keySerializer();
        when(keySerializer.serializeValue(rawKey)).thenReturn(KEY_BYTES);

        //WHEN
        try (MockedStatic<Hashing> hashing = mockStatic(Hashing.class)) {
            hashing.when(Hashing::murmur3_128).thenReturn(hashFunction);
            when(hashFunction.hashBytes(KEY_BYTES)).thenReturn(hashCode);
            when(hashCode.asLong()).thenReturn(KEY_LONG);
            when(store.get(KEY_LONG, CACHE_NAME)).thenReturn(cacheEntry);
            when(cacheEntry.value()).thenReturn(null);
            when(cacheEntry.normalizedKey()).thenReturn(KEY_BYTES);

            var actual = cache.getInternal(keyEntry);

            //THEN returns not null wrapper, but value of the wrapper is null, because null was cached
            assertThat(actual).isNotNull();
            assertThat(actual.get()).isNull();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_incorrect_null_internal() {
        //GIVEN
        when(keyEntry.rawKey()).thenReturn(rawKey);
        doReturn(keySerializer).when(serializerPair).keySerializer();
        when(keySerializer.serializeValue(rawKey)).thenReturn(KEY_BYTES);

        //WHEN
        try (MockedStatic<Hashing> hashing = mockStatic(Hashing.class)) {
            hashing.when(Hashing::murmur3_128).thenReturn(hashFunction);
            when(hashFunction.hashBytes(KEY_BYTES)).thenReturn(hashCode);
            when(hashCode.asLong()).thenReturn(KEY_LONG);
            when(store.get(KEY_LONG, CACHE_NAME)).thenReturn(cacheEntry);
            when(cacheEntry.normalizedKey()).thenReturn(null);

            var actual = cache.getInternal(keyEntry);

            //THEN returns null wrapper, because cached key is different from requested, despite their hashes are the same
            assertThat(actual).isNull();
        }
    }

    @Test
    void should_return_correct_cached_value_internal_typed() {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doReturn(cacheEntry).when(spiedCache).getCacheEntry(keyEntry);
        when(cacheEntry.value()).thenReturn(KEY_BYTES);
        doReturn(VALUE_1).when(spiedCache).deserializeValue(KEY_BYTES, String.class);

        //WHEN
        var actual = spiedCache.getInternal(keyEntry, String.class);

        //THEN returns value (not wrapper) with correct value
        assertThat(actual).isEqualTo(VALUE_1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void should_return_correct_null_value_internal_typed(boolean foundNull) {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doReturn(foundNull ? cacheEntry : null).when(spiedCache).getCacheEntry(keyEntry);
        if (foundNull) {
            when(cacheEntry.value()).thenReturn(null);
        }
        //WHEN
        var actual = spiedCache.getInternal(keyEntry, String.class);

        //THEN returns value (not wrapper) with correct null
        assertThat(actual).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_put_internal() {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doReturn(KEY_BYTES).when(spiedCache).normalizeKey(keyEntry);
        doReturn(KEY_LONG).when(spiedCache).generateKey(KEY_BYTES);
        when(serializerPair.valueSerializer()).thenReturn(valueSerializer);
        when(valueSerializer.serializeValue(VALUE_1)).thenReturn(VALUE_1_BYTES);

        var entry = CacheEntry.builder()
                              .normalizedKey(KEY_BYTES)
                              .value(VALUE_1_BYTES)
                              .build();

        //WHEN
        spiedCache.putInternal(keyEntry, VALUE_1);

        //THEN returns value (not wrapper) with correct null
        verify(store).put(KEY_LONG, entry, TTL_SECONDS, CACHE_NAME);
    }

    @Test
    void should_evict_internal() {
        //GIVEN
        var spiedCache = Mockito.spy(cache);
        doReturn(KEY_BYTES).when(spiedCache).normalizeKey(keyEntry);
        doReturn(KEY_LONG).when(spiedCache).generateKey(KEY_BYTES);

        //WHEN
        spiedCache.evictInternal(keyEntry);

        //THEN
        verify(store).remove(KEY_LONG, CACHE_NAME);
    }

    @Test
    void should_return_same_key_entry() {
        //WHEN
        var actual = cache.keyToKeyEntry(keyEntry);

        //THEN
        assertThat(actual).isEqualTo(keyEntry);
    }

    @Test
    void should_return_created_key_entry() {
        //GIVEN
        var expected = KeyEntry.builder()
                               .rawKey(KEY)
                               .type(null)
                               .build();
        //WHEN
        var actual = cache.keyToKeyEntry(KEY);

        //THEN artificial key-entry is created, because we have dedicated serializer and we do not need type
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_throw_exception_invalid_key() {
        //GIVEN
        var e = new PgCacheKeyException(KEY);
        cache = new PgCacheImpl(CACHE_NAME, store, executorHolder, cacheResilience, generalSerializer, null, properties);

        //WHEN + THEN
        //throws exception, because we do not have both type and dedicated serializer
        assertThatThrownBy(() -> cache.keyToKeyEntry(KEY)).isInstanceOf(PgCacheKeyException.class)
                                                          .hasMessage(e.getMessage());
    }

    @Test
    void should_use_general_serializer_to_serialize_value() {
        //GIVEN
        cache = new PgCacheImpl(CACHE_NAME, store, executorHolder, cacheResilience, generalSerializer, null, properties);
        when(generalSerializer.serialize(VALUE_1)).thenReturn(VALUE_1_BYTES);

        //WHEN
        var actual = cache.serializeValue(VALUE_1);

        //THEN
        assertThat(actual).isEqualTo(VALUE_1_BYTES);
    }

    @Test
    void should_use_general_serializer_to_serialize_key() {
        //GIVEN
        cache = new PgCacheImpl(CACHE_NAME, store, executorHolder, cacheResilience, generalSerializer, null, properties);
        when(generalSerializer.serialize(KEY)).thenReturn(KEY_BYTES);

        //WHEN
        var actual = cache.serializeKey(KEY);

        //THEN
        assertThat(actual).isEqualTo(KEY_BYTES);
    }

    @Test
    void should_use_general_serializer_to_deserialize_value() {
        //GIVEN
        cache = new PgCacheImpl(CACHE_NAME, store, executorHolder, cacheResilience, generalSerializer, null, properties);
        when(generalSerializer.deserialize(VALUE_1_BYTES, (java.lang.reflect.Type) String.class)).thenReturn(VALUE_1);

        //WHEN
        var actual = cache.deserializeValue(VALUE_1_BYTES, String.class);

        //THEN
        assertThat(actual).isEqualTo(VALUE_1);
    }

    @SuppressWarnings("unchecked")
    private <T> void stubCacheResilienceGet() {
        doAnswer(invocation -> {
            //for testing purposes
            Supplier<T> fallback = invocation.getArgument(1);
            fallback.get();
            Supplier<T> supplier = invocation.getArgument(0);
            return supplier.get();
        }).when(cacheResilience).execute(any(Supplier.class), any(Supplier.class));
    }

    private void stubCacheResilienceRun() {
        doAnswer(invocation -> {
            //for testing purposes
            Runnable fallback = invocation.getArgument(1);
            fallback.run();
            Runnable primary = invocation.getArgument(0);
            primary.run();
            return null;
        }).when(cacheResilience).execute(any(Runnable.class), any(Runnable.class));
    }

    private void stubExecutor() {
        doAnswer(invocation -> {
            //for testing purposes
            Runnable primary = invocation.getArgument(0);
            primary.run();
            return null;
        }).when(executor).execute(any(Runnable.class));
    }
}