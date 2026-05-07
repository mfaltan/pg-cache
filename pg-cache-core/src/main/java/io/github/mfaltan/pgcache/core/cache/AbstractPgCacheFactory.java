package io.github.mfaltan.pgcache.core.cache;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.exception.PgCacheSerializerConfigurationException;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.PgCacheGeneralSerializer;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerConfiguration;
import io.github.mfaltan.pgcache.core.store.PgCacheStore;
import io.github.mfaltan.pgcache.resilience.CacheResilience;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.Map;

import static io.github.mfaltan.pgcache.core.cache.PgCacheNoOp.Type.PERMANENTLY;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractPgCacheFactory {

    private final PgCacheStore store;
    private final CacheExecutorHolder executorHolder;
    private final PgCacheGeneralSerializer generalSerializer;
    private final Map<String, PgCacheSerializerConfiguration> serializerConfigurations;
    private final PgCacheProperties properties;

    public PgCache createCache(String name, CacheResilience cacheResilience) {
        boolean cacheDisabled = isCacheDisabled(name);

        if (cacheDisabled) {
            log.debug(Constants.MARKER, "Creating noOp cache [{}]", name);
            return new PgCacheNoOp(name, PERMANENTLY);
        } else {
            log.debug(Constants.MARKER, "Creating new cache [{}]", name);
            var cacheSerializerConfiguration = getSerializerConfiguration(name);
            store.initCache(name);
            return createCache(name, store, executorHolder, cacheResilience, generalSerializer, cacheSerializerConfiguration, properties);
        }
    }

    protected abstract PgCache createCache(String name,
                                           PgCacheStore store,
                                           CacheExecutorHolder cacheExecutorHolder,
                                           CacheResilience cacheResilience,
                                           PgCacheGeneralSerializer generalSerializer,
                                           PgCacheSerializerConfiguration cacheSerializerConfiguration,
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

    protected @Nullable PgCacheSerializerConfiguration getSerializerConfiguration(String name) {
        if (!properties.isCustomSerializers()) {
            return null;
        } else {
            var ret = this.serializerConfigurations.computeIfAbsent(name, (n) -> this.serializerConfigurations.get(null));
            if (ret == null) {
                throw new PgCacheSerializerConfigurationException(name);
            }
            return ret;
        }
    }
}