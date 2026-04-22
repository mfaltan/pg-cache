package io.github.mfaltan.pgcache.core.store;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.core.domain.CacheEntry;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode
@Slf4j
public class NoOpCacheStore implements CacheStore {


    @Override
    public void put(Long key, CacheEntry entry) {
        log.debug(Constants.MARKER, "Not storing key [{}], caching temporarily disabled", key);
    }

    @Override
    public void remove(Long key) {
        log.debug(Constants.MARKER, "Not evicting key [{}], caching temporarily disabled", key);

    }

    @Override
    public void clear() {
        log.debug(Constants.MARKER, "Not clearing cache, caching temporarily disabled");

    }

    @Override
    public CacheEntry get(Long key) {
        log.debug(Constants.MARKER, "Not getting key [{}], caching temporarily disabled", key);
        return null;
    }

    @Override
    public void evictExpired(int limit) {
        log.debug(Constants.MARKER, "Not evicting [{}] expired keys, caching temporarily disabled", limit);
    }
}
