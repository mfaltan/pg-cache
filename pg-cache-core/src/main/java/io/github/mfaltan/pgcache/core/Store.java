package io.github.mfaltan.pgcache.core;

public interface Store {

    void put(Long key, CacheEntry entry);

    void remove(Long key);

    void clear();

    CacheEntry get(Long key);

    void evictExpired(int limit);
}
