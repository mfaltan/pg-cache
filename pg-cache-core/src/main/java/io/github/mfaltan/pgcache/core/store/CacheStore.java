package io.github.mfaltan.pgcache.core.store;

import io.github.mfaltan.pgcache.core.domain.CacheEntry;

public interface CacheStore {

    void put(Long key, CacheEntry entry, int ttlSeconds);

    void remove(Long key);

    void clear();

    CacheEntry get(Long key);

    void evictExpired(int limit);
}
