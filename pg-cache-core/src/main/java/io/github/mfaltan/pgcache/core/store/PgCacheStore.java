package io.github.mfaltan.pgcache.core.store;

import io.github.mfaltan.pgcache.core.domain.CacheEntry;

public interface PgCacheStore {

    void initCache(String name);

    void put(Long key, CacheEntry entry, int ttlSeconds, String name);

    void remove(Long key, String name);

    void clear(String name);

    CacheEntry get(Long key, String name);

    void evictExpired(int limit, String name);
}
