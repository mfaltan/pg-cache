package io.github.mfaltan.pgcache.perftest.client;

import io.github.mfaltan.pgcache.perftest.dto.CacheRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static io.github.mfaltan.pgcache.perftest.Constants.CACHE_1;

@Component
@Slf4j
public class ExternalClient {

    @Cacheable(cacheManager = "pgCacheManager", value = CACHE_1)
    public List<UUID> getDataUsingPgCache(CacheRequest cacheRequest) {
        return getUuids();
    }

    @Cacheable(cacheManager = "redisCacheManager", value = CACHE_1)
    public List<UUID> getDataUsingRedisCache(CacheRequest cacheRequest) {
        return getUuids();
    }

    private static @NonNull List<UUID> getUuids() {
        int count = ThreadLocalRandom.current().nextInt(1, 11);

        return IntStream.range(0, count)
                        .mapToObj(i -> UUID.randomUUID())
                        .toList();
    }
}