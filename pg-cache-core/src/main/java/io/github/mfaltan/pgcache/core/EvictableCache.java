package io.github.mfaltan.pgcache.core;

import org.springframework.cache.Cache;

public interface EvictableCache extends Cache {

    void evictExpired(int limit);
}
