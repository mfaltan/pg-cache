package io.github.mfaltan.pgcache.core.store;

public interface CacheStoreFactory {

    CacheStore initializeStore(String name);
}
