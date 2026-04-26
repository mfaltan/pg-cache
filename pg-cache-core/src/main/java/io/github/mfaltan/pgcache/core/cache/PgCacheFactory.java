package io.github.mfaltan.pgcache.core.cache;

import io.github.mfaltan.pgcache.resilience.CacheResilience;

public interface PgCacheFactory {
    PgCache createCache(String name, CacheResilience cacheResilience);
}
