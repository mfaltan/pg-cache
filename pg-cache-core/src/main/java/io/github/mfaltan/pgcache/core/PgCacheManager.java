package io.github.mfaltan.pgcache.core;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@RequiredArgsConstructor
public class PgCacheManager implements CacheManager {

    private final StoreFactory storeFactory;
    private final ValueSerializer serializer;
    private final Map<String, Cache> caches = new HashMap<>();
    private final Map<String, StoreProperties> storesProperties;

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

                var cache = PgCache.builder()
                                   .name(name)
                                   .serializer(serializer)
                                   .store(store)
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
}
