package io.github.mfaltan.pgcache.core;

import io.github.mfaltan.pgcache.resilience.CacheResilience;
import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgCacheManagerTest {

    private static final String CACHE_NAME_1 = "cache1";
    private static final String CACHE_NAME_2 = "cache2";

    private PgCacheManager cacheManager;

    @Mock
    private StoreFactory storeFactory;

    @Mock
    private ValueSerializer serializer;

    @Mock
    private CacheResilienceFactory cacheResilienceFactory;

    @Mock
    private CacheResilience cacheResilience;

    @Mock
    private Map<String, StoreProperties> storesProperties = new HashMap<>();

    @Mock
    private Store store, store2;

    @Mock
    private StoreProperties storeProperties;

    @BeforeEach
    void init() {
        cacheManager = PgCacheManager.builder()
                                     .storeFactory(storeFactory)
                                     .storesProperties(storesProperties)
                                     .cacheResilienceFactory(cacheResilienceFactory)
                                     .serializer(serializer)
                                     .cleanupEnabled(false)
                                     .cleanupLimit(0)
                                     .cleanupEnabledSupplier(() -> false)
                                     .build();
    }

    @Test
    void should_create_new_cache_when_does_not_exist() {
        // GIVEN
        var expectedCache = createCache();
        when(storesProperties.get(CACHE_NAME_1)).thenReturn(storeProperties);
        when(storeFactory.initializeStore(CACHE_NAME_1, storeProperties)).thenReturn(store);
        when(cacheResilienceFactory.create(CACHE_NAME_1)).thenReturn(cacheResilience);

        // WHEN
        var actual = cacheManager.getCache(CACHE_NAME_1);

        // THEN
        assertThat(actual).isEqualTo(expectedCache);
        verifyNoInteractions(storeProperties);
    }

    @Test
    void should_return_same_instance_when_cache_already_exists() {
        // GIVEN
        var expectedCache = createCache();

        when(storesProperties.get(CACHE_NAME_1)).thenReturn(null);
        when(storeFactory.initializeStore(CACHE_NAME_1, null)).thenReturn(store);
        when(cacheResilienceFactory.create(CACHE_NAME_1)).thenReturn(cacheResilience);

        // WHEN
        var first = cacheManager.getCache(CACHE_NAME_1);
        var second = cacheManager.getCache(CACHE_NAME_1);

        // THEN
        assertThat(second).isSameAs(first);
        assertThat(first).isEqualTo(expectedCache);
        verifyNoMoreInteractions(storeFactory);
    }

    @Test
    void should_create_different_caches_for_different_names() {
        // GIVEN
        when(storesProperties.get(CACHE_NAME_1)).thenReturn(storeProperties);
        when(storesProperties.get(CACHE_NAME_2)).thenReturn(storeProperties);
        when(storeFactory.initializeStore(CACHE_NAME_1, storeProperties)).thenReturn(store);
        when(storeFactory.initializeStore(CACHE_NAME_2, storeProperties)).thenReturn(store2);
        when(cacheResilienceFactory.create(CACHE_NAME_1)).thenReturn(cacheResilience);

        // WHEN
        var cache1 = cacheManager.getCache(CACHE_NAME_1);
        var cache2 = cacheManager.getCache(CACHE_NAME_2);

        // THEN
        assertThat(cache1).isNotEqualTo(cache2);
        assert cache1 != null;
        assertThat(cache1.getName()).isEqualTo(CACHE_NAME_1);
        assert cache2 != null;
        assertThat(cache2.getName()).isEqualTo(CACHE_NAME_2);
    }

    @Test
    void shouldReturnAllCacheNames() {
        // GIVEN
        cacheManager.getCache(CACHE_NAME_1);
        cacheManager.getCache(CACHE_NAME_2);

        // WHEN
        Collection<String> names = cacheManager.getCacheNames();

        // THEN
        assertThat(names).containsExactlyInAnyOrder(CACHE_NAME_1, CACHE_NAME_2);
    }

    @Test
    void should_create_only_one_cache_concurrently() throws InterruptedException {

        when(storesProperties.get(CACHE_NAME_1)).thenReturn(storeProperties);
        when(storeFactory.initializeStore(CACHE_NAME_1, storeProperties)).thenReturn(store);
        when(cacheResilienceFactory.create(CACHE_NAME_1)).thenReturn(cacheResilience);

        int threads = 20;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {

            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            Set<Cache> results = ConcurrentHashMap.newKeySet();

            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        Cache cache = cacheManager.getCache(CACHE_NAME_1);
                        results.add(cache);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            done.await();

            assertThat(results).hasSize(1);
            verify(storeFactory, times(1)).initializeStore(CACHE_NAME_1, storeProperties); //TEST
        }
    }

    private PgCache createCache() {
        return PgCache.builder()
                      .name(CACHE_NAME_1)
                      .serializer(serializer)
                      .store(store)
                      .cacheResilience(cacheResilience)
                      .build();
    }

}