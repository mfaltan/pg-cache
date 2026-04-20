package io.github.mfaltan.pgcache.core;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class NoOpStore implements Store {


    @Override
    public void put(Long key, CacheEntry entry) {
        //do nothing
    }

    @Override
    public void remove(Long key) {
        //do nothing
    }

    @Override
    public void clear() {
        //do nothing
    }

    @Override
    public CacheEntry get(Long key) {
        //do nothing
        return null;
    }

    @Override
    public void evictExpired(int limit) {
        //do nothing
    }
}
