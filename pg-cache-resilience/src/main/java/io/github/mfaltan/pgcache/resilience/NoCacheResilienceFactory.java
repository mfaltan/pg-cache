package io.github.mfaltan.pgcache.resilience;

public class NoCacheResilienceFactory implements CacheResilienceFactory {
    @Override
    public CacheResilience create(String cacheName) {
        return new NoOpCacheResilience();
    }
}
