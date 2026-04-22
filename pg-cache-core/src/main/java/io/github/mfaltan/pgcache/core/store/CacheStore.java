package io.github.mfaltan.pgcache.core.store;

import io.github.mfaltan.pgcache.core.domain.CacheEntry;

public interface CacheStore {

    void put(Long key, CacheEntry entry);

    void remove(Long key);

    void clear();

    CacheEntry get(Long key);

    void evictExpired(int limit);
}
