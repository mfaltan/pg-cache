package io.github.mfaltan.pgcache.core;

import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Builder
public class PgCacheManager implements CacheManager {

    private final StoreFactory storeFactory;
    private final ValueSerializer serializer;
    private final CacheResilienceFactory cacheResilienceFactory;
    private final Map<String, EvictableCache> caches = new HashMap<>();
    private final Map<String, StoreProperties> storesProperties;
    private final boolean cleanupEnabled;
    //in case of multi-node solution, we want to have the possibility, run this only at one node
    private final Supplier<Boolean> cleanupEnabledSupplier;
    private final int cleanupLimit;

    @Override
    public Cache getCache(String name) {
        if (caches.containsKey(name)) {
            return caches.get(name);
        } else {
            synchronized (this) {
                if (caches.containsKey(name)) {
                    return caches.get(name);
                }
                var storeProp = storesProperties.get(name);
                var store = storeFactory.initializeStore(name, storeProp);
                var cacheResilience = cacheResilienceFactory.create(name);

                var cache = PgCache.builder()
                                   .name(name)
                                   .serializer(serializer)
                                   .store(store)
                                   .cacheResilience(cacheResilience)
                                   .build();
                caches.put(name, cache);
                return cache;
            }
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        return new HashSet<>(caches.keySet());
    }

    @Scheduled(cron = "${pg-cache.cleanup-cron:0 */30 * * * *}")
    public void cleanupJob() {
        if (!cleanupEnabled) {
            return;
        }
        var b = cleanupEnabledSupplier.get();
        if (b != null && b) {
            caches.values().forEach(c -> c.evictExpired(cleanupLimit));
        }
    }
}
