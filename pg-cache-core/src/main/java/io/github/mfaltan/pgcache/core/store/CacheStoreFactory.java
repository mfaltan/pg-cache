package io.github.mfaltan.pgcache.core.store;

import io.github.mfaltan.pgcache.common.StoreProperties;

public interface CacheStoreFactory {

    CacheStore initializeStore(String name, StoreProperties storeProperties);
}
