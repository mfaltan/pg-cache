package io.github.mfaltan.pgcache.resilience;

public interface CacheResilienceFactory {
    CacheResilience create(String cacheName);
}
