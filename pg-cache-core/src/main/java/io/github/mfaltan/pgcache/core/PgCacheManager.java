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

    private final ValueSerializer serializer;
    private final Map<String, Cache> caches = new HashMap<>();

    @Override
    public Cache getCache(String name) {
        if (caches.containsKey(name)) {
            return caches.get(name);
        } else {
            synchronized (this) {
                if (caches.containsKey(name)) {
                    return caches.get(name);
                }
                var cache = PgCache.builder()
                                   .name(name)
                                   .serializer(serializer)
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
