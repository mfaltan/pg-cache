package io.github.mfaltan.pgcache.core.cache;

import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.PgCacheGeneralSerializer;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerConfiguration;
import io.github.mfaltan.pgcache.core.store.PgCacheStore;
import io.github.mfaltan.pgcache.resilience.CacheResilience;

import java.util.Map;

public class PgCacheFactoryImpl extends AbstractPgCacheFactory implements PgCacheFactory {

    public PgCacheFactoryImpl(PgCacheStore store,
                              CacheExecutorHolder executorHolder,
                              PgCacheGeneralSerializer generalSerializer,
                              Map<String, PgCacheSerializerConfiguration> serializerConfigurations,
                              PgCacheProperties properties) {
        super(store, executorHolder, generalSerializer, serializerConfigurations, properties);
    }

    @Override
    protected PgCache createCache(String name,
                                  PgCacheStore store,
                                  CacheExecutorHolder executorHolder,
                                  CacheResilience resilience,
                                  PgCacheGeneralSerializer serializer,
                                  PgCacheSerializerConfiguration cacheSerializerConfiguration,
                                  PgCacheProperties properties) {
        return new PgCacheImpl(name, store, executorHolder, resilience, serializer, cacheSerializerConfiguration, properties);
    }
}
