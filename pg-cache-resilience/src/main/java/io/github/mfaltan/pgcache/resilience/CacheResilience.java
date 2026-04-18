package io.github.mfaltan.pgcache.resilience;

import java.util.function.Supplier;

public interface CacheResilience {
    <T> T execute(Supplier<T> primary, Supplier<T> fallback);

    void execute(Runnable primary, Runnable fallback);
}
