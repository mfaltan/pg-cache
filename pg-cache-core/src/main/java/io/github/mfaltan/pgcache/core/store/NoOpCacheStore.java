package io.github.mfaltan.pgcache.core.store;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.core.domain.CacheEntry;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@EqualsAndHashCode
@Slf4j
public class NoOpCacheStore implements CacheStore {

    private final boolean temporary;

    @Override
    public void put(Long key, CacheEntry entry) {
        log.debug(Constants.MARKER, "Not storing key [{}], caching {} disabled", key, getMessage());
    }

    @Override
    public void remove(Long key) {
        log.debug(Constants.MARKER, "Not evicting key [{}], caching {} disabled", key, getMessage());

    }

    @Override
    public void clear() {
        log.debug(Constants.MARKER, "Not clearing cache, caching {} disabled", getMessage());

    }

    @Override
    public CacheEntry get(Long key) {
        log.debug(Constants.MARKER, "Not getting key [{}], caching {} disabled", key, getMessage());
        return null;
    }

    @Override
    public void evictExpired(int limit) {
        log.debug(Constants.MARKER, "Not evicting [{}] expired keys, caching {} disabled", limit, getMessage());
    }

    private String getMessage() {
        return temporary ? "temporarily" : "";
    }
}
