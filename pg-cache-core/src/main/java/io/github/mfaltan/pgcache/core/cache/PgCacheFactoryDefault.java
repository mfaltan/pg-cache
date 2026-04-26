package io.github.mfaltan.pgcache.core.cache;

import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.CacheValueSerializer;
import io.github.mfaltan.pgcache.core.store.CacheStore;
import io.github.mfaltan.pgcache.core.store.CacheStoreFactory;
import io.github.mfaltan.pgcache.resilience.CacheResilience;

import java.util.List;

public class PgCacheFactoryDefault extends AbstractPgCacheFactory implements PgCacheFactory {

    public PgCacheFactoryDefault(CacheStoreFactory storeFactory,
                                 CacheExecutorHolder executorHolder,
                                 List<CacheValueSerializer> serializers,
                                 PgCacheProperties properties) {
        super(storeFactory, executorHolder, serializers, properties);
    }

    @Override
    protected PgCache createCache(String name,
                                  CacheStore store,
                                  CacheExecutorHolder executorHolder,
                                  CacheResilience resilience,
                                  CacheValueSerializer serializer) {
        return new PgCacheDefault(name, store, executorHolder, resilience, serializer);
    }
}
