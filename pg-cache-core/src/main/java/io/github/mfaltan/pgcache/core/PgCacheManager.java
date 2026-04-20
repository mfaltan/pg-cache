package io.github.mfaltan.pgcache.core;

import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.resilience.CacheResilience;
import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import io.github.mfaltan.pgcache.resilience.NoOpCacheResilience;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Builder
public class PgCacheManager implements CacheManager {

    private final StoreFactory storeFactory;
    private final ValueSerializer serializer;
    private final CacheResilienceFactory cacheResilienceFactory;
    private final PgCacheProperties properties;

    private final Map<String, EvictableCache> caches = new HashMap<>();

    @Override
    public Cache getCache(String name) {
        if (caches.containsKey(name)) {
            return caches.get(name);
        } else {
            synchronized (this) {
                if (caches.containsKey(name)) {
                    return caches.get(name);
                }
                var cacheResilience = cacheResilienceFactory.create(name);
                return cacheResilience.execute(() -> createAndRegisterRealCache(name, cacheResilience), () -> createNoOpCache(name));
            }
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        return new HashSet<>(caches.keySet());
    }

    @Scheduled(cron = "${pg-cache.cleanup-cron:0 */30 * * * *}")
    public void cleanupJob() {
        if (!properties.isCleanupEnabled()) {
            return;
        }
        caches.values().forEach(c -> c.evictExpired(properties.getCleanupLimit()));
    }

    private PgCache createAndRegisterRealCache(String name, CacheResilience cacheResilience) {
        var storeProp = properties.getCaches().get(name);
        var store = storeFactory.initializeStore(name, storeProp);
        var cache = PgCache.builder()
                           .name(name)
                           .serializer(serializer)
                           .store(store)
                           .cacheResilience(cacheResilience)
                           .build();
        caches.put(name, cache);
        return cache;
    }

    private PgCache createNoOpCache(String name) {
        return PgCache.builder()
                      .name(name)
                      .serializer(serializer)
                      .store(new NoOpStore())
                      .cacheResilience(new NoOpCacheResilience())
                      .build();
    }
}
