package io.github.mfaltan.pgcache.core.cache;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.PgCacheGeneralSerializer;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerConfiguration;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerPair;
import io.github.mfaltan.pgcache.core.store.PgCacheStore;
import io.github.mfaltan.pgcache.resilience.CacheResilience;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import static io.github.mfaltan.pgcache.core.cache.PgCacheNoOp.Type.PERMANENTLY;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractPgCacheFactory {

    private final PgCacheStore store;
    private final CacheExecutorHolder executorHolder;
    private final PgCacheGeneralSerializer generalSerializer;
    private final PgCacheSerializerConfiguration serializerConfiguration;
    private final PgCacheProperties properties;

    public PgCache createCache(String name, CacheResilience cacheResilience) {
        boolean cacheDisabled = isCacheDisabled(name);

        if (cacheDisabled) {
            log.debug(Constants.MARKER, "Creating noOp cache [{}]", name);
            return new PgCacheNoOp(name, PERMANENTLY);
        } else {
            log.debug(Constants.MARKER, "Creating new cache [{}]", name);
            var cacheSerializerPair = getSerializerConfiguration(name);
            store.initCache(name);
            return createCache(name, store, executorHolder, cacheResilience, generalSerializer, cacheSerializerPair, properties);
        }
    }

    protected abstract PgCache createCache(String name,
                                           PgCacheStore store,
                                           CacheExecutorHolder cacheExecutorHolder,
                                           CacheResilience cacheResilience,
                                           PgCacheGeneralSerializer generalSerializer,
                                           PgCacheSerializerPair cacheSerializerPair,
                                           PgCacheProperties properties);

    protected @Nonnull Boolean isCacheDisabled(@NonNull String name) {
        return properties.getCaches()
                         .entrySet()
                         .stream()
                         .filter(entry -> entry.getKey().equals(name))
                         .map(entry -> entry.getValue().isDisabled())
                         .findFirst()
                         .orElse(false);
    }

    protected @Nullable PgCacheSerializerPair getSerializerConfiguration(String name) {
        if (!properties.isCustomSerializers()) {
            return null;
        } else {
            return serializerConfiguration.getSerializerPair(name);
        }
    }
}