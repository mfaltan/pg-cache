package io.github.mfaltan.pgcache.core;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.cache.PgCache;
import io.github.mfaltan.pgcache.core.cache.PgCacheFactory;
import io.github.mfaltan.pgcache.core.cache.PgCacheNoOp;
import io.github.mfaltan.pgcache.resilience.CacheResilience;
import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import static io.github.mfaltan.pgcache.core.cache.PgCacheNoOp.Type.TEMPORARILY;

@Slf4j
@RequiredArgsConstructor
public class PgCacheManager implements CacheManager {

    private final PgCacheFactory pgCacheFactory;
    private final CacheResilienceFactory cacheResilienceFactory;
    private final PgCacheProperties properties;

    private final Map<String, PgCache> caches = new HashMap<>();

    @PostConstruct
    public void init() {
        //TODO here, all the configured caches are initialized. But later, other caches might be initialized on demand
    }

    @Override
    public Cache getCache(@Nonnull String name) {
        if (caches.containsKey(name)) {
            log.debug(Constants.MARKER, "Using already existing cache [{}]", name);
            return caches.get(name);
        } else {
            var cacheResilience = cacheResilienceFactory.create(name);
            var newCache = cacheResilience.execute(() -> this.getNewCache(name, cacheResilience), () -> new PgCacheNoOp(name, TEMPORARILY));
            if (!(newCache instanceof PgCacheNoOp)) {
                caches.put(name, newCache);
            }
            return newCache;
        }
    }

    private PgCache getNewCache(String name, CacheResilience cacheResilience) {
        // this is executed behind circuit breaker, if there is some issue with storage, alternate circuit will be opened and it will not get here
        synchronized (this) {
            if (caches.containsKey(name)) {
                log.debug(Constants.MARKER, "Using already existing cache [{}], it was created in the meantime", name);
                return caches.get(name);
            }
            return pgCacheFactory.createCache(name, cacheResilience);
        }
    }

    @Override
    @Nonnull
    public Collection<String> getCacheNames() {
        return new HashSet<>(caches.keySet());
    }

    @Scheduled(cron = "${pg-cache.cleanup-cron:0 */30 * * * *}")
    public void cleanupJob() {
        MDC.put(properties.getTraceIdKey(), UUID.randomUUID().toString());
        try {
            if (!properties.isCleanupEnabled()) {
                log.debug(Constants.MARKER, "Cleanup job disabled");
                return;
            }
            log.debug(Constants.MARKER, "About to execute cleanup job for caches [{}]", caches.keySet());
            caches.values().forEach(c -> c.evictExpired(properties.getCleanupLimit()));
        } finally {
            MDC.clear();
        }
    }
}
