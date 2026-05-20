package io.github.mfaltan.pgcache.core.cache;

import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.PgCacheGeneralSerializer;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerPair;
import io.github.mfaltan.pgcache.core.store.PgCacheStore;
import io.github.mfaltan.pgcache.resilience.CacheResilience;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgCacheFactoryImplTest extends AbstractPgCacheFactoryTest{

    @BeforeEach
    void init() {
        this.factory = new PgCacheFactoryImpl(store, executorHolder, generalSerializer, serializerConfiguration, properties);
    }

    @Override
    PgCacheImpl createExpectedCache(String name, PgCacheStore store, CacheExecutorHolder cacheExecutorHolder, CacheResilience cacheResilience, PgCacheGeneralSerializer generalSerializer, PgCacheSerializerPair cacheSerializerPair, PgCacheProperties properties) {
        return new PgCacheImpl(name, store, executorHolder, cacheResilience, generalSerializer, cacheSerializerPair, properties);
    }
}