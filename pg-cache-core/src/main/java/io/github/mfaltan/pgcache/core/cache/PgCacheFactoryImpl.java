package io.github.mfaltan.pgcache.core.cache;

import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.PgCacheGeneralSerializer;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerConfiguration;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerPair;
import io.github.mfaltan.pgcache.core.store.PgCacheStore;
import io.github.mfaltan.pgcache.resilience.CacheResilience;

public class PgCacheFactoryImpl extends AbstractPgCacheFactory implements PgCacheFactory {

    public PgCacheFactoryImpl(PgCacheStore store,
                              CacheExecutorHolder executorHolder,
                              PgCacheGeneralSerializer generalSerializer,
                              PgCacheSerializerConfiguration serializerConfiguration,
                              PgCacheProperties properties) {
        super(store, executorHolder, generalSerializer, serializerConfiguration, properties);
    }

    @Override
    protected PgCache createCache(String name,
                                  PgCacheStore store,
                                  CacheExecutorHolder executorHolder,
                                  CacheResilience resilience,
                                  PgCacheGeneralSerializer serializer,
                                  PgCacheSerializerPair serializerPair,
                                  PgCacheProperties properties) {
        return new PgCacheImpl(name, store, executorHolder, resilience, serializer, serializerPair, properties);
    }
}
