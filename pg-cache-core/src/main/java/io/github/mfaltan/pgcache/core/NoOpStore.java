package io.github.mfaltan.pgcache.core;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class NoOpStore implements Store {


    @Override
    public void put(Long key, CacheEntry entry) {
    }

    @Override
    public void remove(Long key) {
    }

    @Override
    public void clear() {
    }

    @Override
    public CacheEntry get(Long key) {
        return null;
    }

    @Override
    public void evictExpired(int limit) {
    }
}
