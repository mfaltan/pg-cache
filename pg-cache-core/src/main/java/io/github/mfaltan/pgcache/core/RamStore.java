package io.github.mfaltan.pgcache.core;

import lombok.EqualsAndHashCode;

import java.util.concurrent.ConcurrentHashMap;

@EqualsAndHashCode
public class RamStore implements Store {

    protected final ConcurrentHashMap<Long, CacheEntry> data = new ConcurrentHashMap<>();

    @Override
    public void put(Long key, CacheEntry entry) {
        data.put(key, entry);
    }

    @Override
    public void remove(Long key) {
        data.remove(key);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public CacheEntry get(Long key) {
        return data.get(key);
    }
}
