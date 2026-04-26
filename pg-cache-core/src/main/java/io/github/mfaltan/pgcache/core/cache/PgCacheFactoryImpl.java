package io.github.mfaltan.pgcache.core.cache;

import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.CacheValueSerializer;
import io.github.mfaltan.pgcache.core.store.PgCacheStore;
import io.github.mfaltan.pgcache.resilience.CacheResilience;

import java.util.List;

public class PgCacheFactoryImpl extends AbstractPgCacheFactory implements PgCacheFactory {

    public PgCacheFactoryImpl(PgCacheStore store,
                              CacheExecutorHolder executorHolder,
                              List<CacheValueSerializer> serializers,
                              PgCacheProperties properties) {
        super(store, executorHolder, serializers, properties);
    }

    @Override
    protected PgCache createCache(String name,
                                  PgCacheStore store,
                                  CacheExecutorHolder executorHolder,
                                  CacheResilience resilience,
                                  CacheValueSerializer serializer,
                                  PgCacheProperties properties) {
        return new PgCacheImpl(name, store, executorHolder, resilience, serializer, properties);
    }
}
