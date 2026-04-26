package io.github.mfaltan.pgcache.core.cache;

import org.springframework.cache.Cache;

public interface PgCache extends Cache {

    void evictExpired(int limit);
}
