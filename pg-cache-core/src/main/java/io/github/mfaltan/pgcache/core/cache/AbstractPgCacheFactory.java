package io.github.mfaltan.pgcache.core.cache;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.CacheValueSerializer;
import io.github.mfaltan.pgcache.core.store.PgCacheStore;
import io.github.mfaltan.pgcache.resilience.CacheResilience;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.mfaltan.pgcache.core.cache.PgCacheNoOp.Type.PERMANENTLY;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractPgCacheFactory {

    protected final PgCacheStore store;
    protected final CacheExecutorHolder executorHolder;
    protected final List<CacheValueSerializer> serializers;
    protected final PgCacheProperties properties;

    protected final Map<String, CacheValueSerializer> serializerMap = new HashMap<>();

    @PostConstruct
    public void init() {

        for (var serializer : serializers) {
            var cacheNames = serializer.getCacheNames();

            // null means: use null as a single key - default serializer
            if (cacheNames == null) {
                putOrThrow(serializerMap, null, serializer);
                continue;
            }

            for (var cacheName : cacheNames) {
                putOrThrow(serializerMap, cacheName, serializer);
            }
        }
    }

    public PgCache createCache(String name, CacheResilience cacheResilience) {
        var cacheDisabled = isCacheDisabled(name);

        if (cacheDisabled) {
            log.debug(Constants.MARKER, "Creating noOp cache [{}]", name);
            return new PgCacheNoOp(name, PERMANENTLY);
        } else {
            log.debug(Constants.MARKER, "Creating new cache [{}]", name);
            store.initCache(name);
            var serializer = getSerializer(name);
            return createCache(name, store, executorHolder, cacheResilience, serializer, properties);
        }
    }

    protected abstract PgCache createCache(String name,
                                           PgCacheStore store,
                                           CacheExecutorHolder cacheExecutorHolder,
                                           CacheResilience cacheResilience,
                                           CacheValueSerializer serializer,
                                           PgCacheProperties properties);

    protected @NonNull Boolean isCacheDisabled(@NonNull String name) {
        return properties.getCaches()
                         .entrySet()
                         .stream()
                         .filter(entry -> entry.getKey().equals(name))
                         .map(entry -> entry.getValue().isDisabled())
                         .findFirst()
                         .orElse(false);
    }

    protected <K, V> void putOrThrow(Map<K, V> map, K key, V value) {
        if (map.containsKey(key)) {
            throw new IllegalStateException("Duplicate cacheName detected: " + key);
        }

        map.put(key, value);
    }

    private CacheValueSerializer getSerializer(String name) {
        return serializerMap.computeIfAbsent(name, (n) -> serializerMap.get(null));
    }
}
