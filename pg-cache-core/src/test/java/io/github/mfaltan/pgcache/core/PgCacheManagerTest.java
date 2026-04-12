package io.github.mfaltan.pgcache.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PgCacheManagerTest {

    private static final String CACHE_NAME_1 = "cache1";
    private static final String CACHE_NAME_2 = "cache2";

    @InjectMocks
    private PgCacheManager cacheManager;

    @Mock
    private ValueSerializer serializer;

    @Test
    void should_create_new_cache_when_does_not_exist() {
        // GIVEN
        var expectedCache = PgCache.builder()
                                   .name(CACHE_NAME_1)
                                   .serializer(serializer)
                                   .build();

        // WHEN
        var actual = cacheManager.getCache(CACHE_NAME_1);

        // THEN
        assertThat(actual).isEqualTo(expectedCache);
    }

    @Test
    void should_return_same_instance_when_cache_already_exists() {
        // GIVEN
        var expectedCache = PgCache.builder()
                                   .name(CACHE_NAME_1)
                                   .serializer(serializer)
                                   .build();

        // WHEN
        var first = cacheManager.getCache(CACHE_NAME_1);
        var second = cacheManager.getCache(CACHE_NAME_1);

        // THEN
        assertThat(second).isSameAs(first);
        assertThat(first).isEqualTo(expectedCache);
    }

    @Test
    void should_create_different_caches_for_different_names() {
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
    void should_create_only_one_cache_concurrently() throws Exception {

        int threads = 20;
        try(ExecutorService executor = Executors.newFixedThreadPool(threads)) {

            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            Set<Cache> results = ConcurrentHashMap.newKeySet();

            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        Cache cache = cacheManager.getCache(CACHE_NAME_1);
                        results.add(cache);
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            done.await();

            assertThat(results).hasSize(1);
        }
    }
}