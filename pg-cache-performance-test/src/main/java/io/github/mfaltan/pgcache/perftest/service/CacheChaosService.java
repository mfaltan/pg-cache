package io.github.mfaltan.pgcache.perftest.service;

import io.github.mfaltan.pgcache.core.PgCacheManager;
import io.github.mfaltan.pgcache.perftest.client.ExternalClient;
import io.github.mfaltan.pgcache.perftest.dto.CacheRequest;
import io.github.mfaltan.pgcache.perftest.dto.TestResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.github.mfaltan.pgcache.perftest.Constants.CACHE_1;

@RequiredArgsConstructor
@Service
public class CacheChaosService {

    private final PgCacheManager pgCacheManager;
    private final RedisCacheManager redisCacheManager;
    private final ExternalClient externalClient;


    public TestResult executeTest(boolean usePgCache) {
        int writes = 200000;

        var cacheManager = usePgCache ? pgCacheManager : redisCacheManager;
        var cache = cacheManager.getCache(CACHE_1);
        assert cache != null;
        cache.clear();

        List<CacheRequest> testData = new ArrayList<>();
        for (int i = 0; i < writes; i++) {
            for (int j = 0; j < 5; j++) {
                var testDto = new CacheRequest(i, i, "name");
                testData.add(testDto);
            }
        }
        Collections.shuffle(testData);

        var startTime = LocalDateTime.now();
        testData.stream()
                .parallel()
                .forEach(t -> {
                    if (usePgCache) {
                        externalClient.getDataUsingPgCache(t);
                    } else {
                        externalClient.getDataUsingRedisCache(t);
                    }
                });

        var endTime = LocalDateTime.now();
        var diffMillis = Duration.between(startTime, endTime).toMillis();

        double opsPerSec = (testData.size() * 1_000.0) / diffMillis;

        return new TestResult(diffMillis, opsPerSec);
    }
}
