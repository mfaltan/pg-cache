package io.github.mfaltan.pgcache.resilience;

import io.github.mfaltan.pgcache.common.Constants;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class NoOpCacheResilience implements CacheResilience{
    @Override
    public <T> T execute(Supplier<T> primary, Supplier<T> fallback) {
        try {
            return primary.get();
        } catch (RuntimeException e) {
            log.warn(Constants.MARKER, "Problem with pgCache, returning null", e);
            return null;
        }
    }

    @Override
    public void execute(Runnable primary, Runnable fallback) {
        try {
            primary.run();
        } catch (RuntimeException e) {
            log.warn(Constants.MARKER, "Problem with pgCache, returning null", e);
        }
    }
}
