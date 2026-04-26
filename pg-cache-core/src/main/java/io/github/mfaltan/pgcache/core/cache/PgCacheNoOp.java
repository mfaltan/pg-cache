package io.github.mfaltan.pgcache.core.cache;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.core.exception.PgCacheCallerException;
import jakarta.annotation.Nonnull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Builder
@RequiredArgsConstructor
@Slf4j
public class PgCacheNoOp implements PgCache {

    private final String name;
    private final Type type;

    @Override
    @Nonnull
    public String getName() {
        return name;
    }

    @Override
    @Nonnull
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(@Nonnull Object key) {
        log.debug(Constants.MARKER, "Cache [{}] [{}] disabled, not getting value for key [{}]", name, type, key);
        return null;
    }

    @Override
    public <T> T get(@Nonnull Object key, Class<T> type) {
        log.debug(Constants.MARKER, "Cache [{}] [{}] disabled, not getting value for key [{}] and class [{}]", name, type, key, type);
        return null;
    }

    @Override
    public <T> T get(@Nonnull Object key, @Nonnull Callable<T> valueLoader) {
        try {
            log.debug(Constants.MARKER, "Cache [{}] [{}] disabled, not getting value for key [{}], directly calling valueLoader", name, type, key);
            return valueLoader.call();
        } catch (Exception e) {
            throw new PgCacheCallerException(e);
        }
    }

    @Override
    public void put(@Nonnull Object key, Object value) {
        log.debug(Constants.MARKER, "Cache [{}] [{}] disabled, not storing value for key [{}]", name, type, key);
    }

    @Override
    public void evict(@Nonnull Object key) {
        log.debug(Constants.MARKER, "Cache [{}] [{}] disabled, not evicting key [{}]", name, type, key);
    }

    @Override
    public void clear() {
        log.debug(Constants.MARKER, "Cache [{}] [{}] disabled, not clearing", name, type);
    }

    @Override
    public void evictExpired(int limit) {
        log.debug(Constants.MARKER, "Cache [{}] [{}] disabled, not evicting [{}]", name, type, limit);
    }

    public enum Type {
        PERMANENTLY,
        TEMPORARILY
    }
}
