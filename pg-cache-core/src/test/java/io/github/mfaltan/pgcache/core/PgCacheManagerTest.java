package io.github.mfaltan.pgcache.core;

import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.cache.PgCache;
import io.github.mfaltan.pgcache.core.cache.PgCacheFactory;
import io.github.mfaltan.pgcache.core.cache.PgCacheNoOp;
import io.github.mfaltan.pgcache.resilience.CacheResilience;
import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static io.github.mfaltan.pgcache.core.cache.PgCacheNoOp.Type.TEMPORARILY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgCacheManagerTest {

    private static final String CACHE_NAME_1 = "cacheName1";
    private static final String TRACE_ID_KEY = "traceIdKey";
    private static final UUID TRACE_ID_VALUE = new UUID(1L, 1L);
    private static final int CLEANUP_LIMIT = 100;

    @InjectMocks
    private PgCacheManager cacheManager;

    @Mock
    private PgCacheFactory pgCacheFactory;
    @Mock
    private CacheResilienceFactory cacheResilienceFactory;
    @Mock
    private PgCacheProperties properties;
    @Mock
    private PgCache cache1;
    @Mock
    private CacheResilience cacheResilience;

    @AfterEach
    void verifyNoUnexpectedInteractions() {
        Mockito.verifyNoMoreInteractions(
                pgCacheFactory,
                cacheResilienceFactory,
                properties,
                cache1);
    }

    @Test
    void should_not_crash_when_init() {
        //WHEN + THEN
        assertThatCode(() -> cacheManager.init()).doesNotThrowAnyException();
    }

    @Test
    void should_return_unmodifiable_cache_names() {
        //GIVEN
        cacheManager.caches.put(CACHE_NAME_1, null);
        var expected = Set.of(CACHE_NAME_1);

        //WHEN + THEN
        var actual = cacheManager.getCacheNames();
        assertThatThrownBy(() -> actual.add("something")).isInstanceOf(UnsupportedOperationException.class);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_return_found_cache() {
        //GIVEN
        cacheManager.caches.put(CACHE_NAME_1, cache1);

        //WHEN
        var actual = cacheManager.getCache(CACHE_NAME_1);

        //THEN
        assertThat(actual).isEqualTo(cache1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_created_cache() {
        //GIVEN
        var expectedNoOpCache = new PgCacheNoOp(CACHE_NAME_1, TEMPORARILY);
        var answer = createCacheResilienceAnswer(expectedNoOpCache, true);
        when(cacheResilienceFactory.create(CACHE_NAME_1)).thenReturn(cacheResilience);
        when(pgCacheFactory.createCache(CACHE_NAME_1, cacheResilience)).thenReturn(cache1);
        when(cacheResilience.execute(any(Supplier.class), any(Supplier.class))).thenAnswer(answer);

        //WHEN
        var actual = cacheManager.getCache(CACHE_NAME_1);

        //THEN
        assertThat(actual).isEqualTo(cache1);
        assertThat(cacheManager.caches).isEqualTo(Map.of(CACHE_NAME_1, cache1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_created_cache_concurrently_created() {
        //GIVEN
        var expectedNoOpCache = new PgCacheNoOp(CACHE_NAME_1, TEMPORARILY);
        var answer = createCacheResilienceAnswer(expectedNoOpCache, true);
        when(cacheResilienceFactory.create(CACHE_NAME_1)).thenAnswer((Answer<CacheResilience>) invocation -> {
            //to simulate, that someone else in the meantime created the cache
            cacheManager.caches.put(CACHE_NAME_1, cache1);
            return cacheResilience;
        });
        when(cacheResilience.execute(any(Supplier.class), any(Supplier.class))).thenAnswer(answer);

        //WHEN
        var actual = cacheManager.getCache(CACHE_NAME_1);

        //THEN
        assertThat(actual).isEqualTo(cache1);
        assertThat(cacheManager.caches).isEqualTo(Map.of(CACHE_NAME_1, cache1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_noop_cache() {
        //GIVEN
        var expectedNoOpCache = new PgCacheNoOp(CACHE_NAME_1, TEMPORARILY);
        var answer = createCacheResilienceAnswer(expectedNoOpCache, false);
        when(cacheResilienceFactory.create(CACHE_NAME_1)).thenReturn(cacheResilience);
        when(cacheResilience.execute(any(Supplier.class), any(Supplier.class))).thenAnswer(answer);

        //WHEN
        var actual = cacheManager.getCache(CACHE_NAME_1);

        //THEN
        assertThat(actual).isEqualTo(expectedNoOpCache);
        assertThat(cacheManager.caches).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_return_null_cache_fallback() {
        //GIVEN
        var expectedNoOpCache = new PgCacheNoOp(CACHE_NAME_1, TEMPORARILY);
        var answer = createCacheResilienceAnswer(null, false);
        when(cacheResilienceFactory.create(CACHE_NAME_1)).thenReturn(cacheResilience);
        when(cacheResilience.execute(any(Supplier.class), any(Supplier.class))).thenAnswer(answer);

        //WHEN
        var actual = cacheManager.getCache(CACHE_NAME_1);

        //THEN
        assertThat(actual).isEqualTo(expectedNoOpCache);
        assertThat(cacheManager.caches).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void should_execute_cleanup(boolean enabled) {
        //GIVEN
        when(properties.getTraceIdKey()).thenReturn(TRACE_ID_KEY);
        when(properties.isCleanupEnabled()).thenReturn(enabled);

        //WHEN + THEN
        try (MockedStatic<MDC> mockedMDC = Mockito.mockStatic(MDC.class);
             MockedStatic<UUID> mockedUUID = Mockito.mockStatic(UUID.class)) {

            mockedUUID.when(UUID::randomUUID).thenReturn(TRACE_ID_VALUE);

            cacheManager.caches.put(CACHE_NAME_1, cache1);
            if (enabled) {
                when(properties.getCleanupLimit()).thenReturn(CLEANUP_LIMIT);
            }

            cacheManager.cleanupJob();

            if (enabled) {
                verify(cache1).evictExpired(CLEANUP_LIMIT);
            }

            mockedMDC.verify(() -> MDC.put(TRACE_ID_KEY, TRACE_ID_VALUE.toString()));
            mockedMDC.verify(MDC::clear);
        }
    }

    @SuppressWarnings("unchecked")
    private static Answer<PgCache> createCacheResilienceAnswer(PgCacheNoOp noOpCache, boolean primary) {
        return invocation -> {
            var supplier = (Supplier<PgCache>) invocation.getArguments()[0];
            var supplierNoOp = (Supplier<PgCache>) invocation.getArguments()[1];

            if (primary) {
                var actualNoOpCache = supplierNoOp.get();
                assertThat(actualNoOpCache).isEqualTo(noOpCache);
                return supplier.get();
            } else {
                return noOpCache;
            }
        };
    }
}