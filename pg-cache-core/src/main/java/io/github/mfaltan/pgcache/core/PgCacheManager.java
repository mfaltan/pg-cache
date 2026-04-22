package io.github.mfaltan.pgcache.core;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.cache.EvictableCache;
import io.github.mfaltan.pgcache.core.cache.PgCache;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.CacheValueSerializer;
import io.github.mfaltan.pgcache.core.store.CacheStoreFactory;
import io.github.mfaltan.pgcache.core.store.NoOpCacheStore;
import io.github.mfaltan.pgcache.resilience.CacheResilience;
import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import io.github.mfaltan.pgcache.resilience.NoOpCacheResilience;
import jakarta.annotation.Nonnull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Builder
public class PgCacheManager implements CacheManager {

    private final CacheExecutorHolder cacheExecutorHolder;
    private final CacheStoreFactory cacheStoreFactory;
    private final CacheValueSerializer serializer;
    private final CacheResilienceFactory cacheResilienceFactory;
    private final PgCacheProperties properties;

    private final Map<String, EvictableCache> caches = new HashMap<>();

    @Override
    public Cache getCache(@Nonnull String name) {
        if (caches.containsKey(name)) {
            log.debug(Constants.MARKER, "Using already existing cache [{}]", name);
            return caches.get(name);
        } else {
            synchronized (this) {
                if (caches.containsKey(name)) {
                    log.debug(Constants.MARKER, "Using already existing cache [{}], it was created in the meantime", name);
                    return caches.get(name);
                }
                log.debug(Constants.MARKER, "Creating new cache [{}]", name);
                var cacheResilience = cacheResilienceFactory.create(name);
                return cacheResilience.execute(() -> createAndRegisterRealCache(name, cacheResilience), () -> createNoOpCache(name));
            }
        }
    }

    @Override
    @Nonnull
    public Collection<String> getCacheNames() {
        return new HashSet<>(caches.keySet());
    }

    @Scheduled(cron = "${pg-cache.cleanup-cron:0 */30 * * * *}")
    public void cleanupJob() {
        MDC.put(properties.getTraceIdKey(), UUID.randomUUID().toString());
        try {
            if (!properties.isCleanupEnabled()) {
                log.debug(Constants.MARKER, "Cleanup job disabled");
                return;
            }
            log.debug(Constants.MARKER, "About to execute cleanup job for caches [{}]", caches.keySet());
            caches.values().forEach(c -> c.evictExpired(properties.getCleanupLimit()));
        } finally {
            MDC.clear();
        }
    }

    private PgCache createAndRegisterRealCache(String name, CacheResilience cacheResilience) {
        var storeProp = properties.getCaches().get(name);
        var store = cacheStoreFactory.initializeStore(name, storeProp);
        var cache = PgCache.builder()
                           .name(name)
                           .cacheExecutorHolder(cacheExecutorHolder)
                           .serializer(serializer)
                           .cacheStore(store)
                           .cacheResilience(cacheResilience)
                           .build();
        caches.put(name, cache);
        return cache;
    }

    private PgCache createNoOpCache(String name) {
        return PgCache.builder()
                      .name(name)
                      .serializer(serializer)
                      .cacheStore(new NoOpCacheStore())
                      .cacheResilience(new NoOpCacheResilience())
                      .build();
    }
}
