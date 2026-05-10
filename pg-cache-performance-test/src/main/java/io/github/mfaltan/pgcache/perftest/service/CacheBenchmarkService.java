package io.github.mfaltan.pgcache.perftest.service;

import io.github.mfaltan.pgcache.core.PgCacheManager;
import io.github.mfaltan.pgcache.perftest.client.ExternalClient;
import io.github.mfaltan.pgcache.perftest.dto.CacheRequest;
import io.github.mfaltan.pgcache.perftest.dto.TestResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.github.mfaltan.pgcache.perftest.Constants.CACHE_1;

@RequiredArgsConstructor
@Service
@Slf4j
public class CacheBenchmarkService {

    private final PgCacheManager pgCacheManager;
    private final RedisCacheManager redisCacheManager;
    private final ExternalClient externalClient;


    public TestResult executeBenchmark(boolean usePgCache) {

        int writes = 200_000;
        int iterationsPerKey = 5;

        var cacheManager = usePgCache ? pgCacheManager : redisCacheManager;
        var cache = cacheManager.getCache(CACHE_1);
        assert cache != null;

        cache.clear();

        // 1. Prepare test data (deterministic)
        List<CacheRequest> testData = new ArrayList<>(writes * iterationsPerKey);

        for (int i = 0; i < writes; i++) {
            for (int j = 0; j < iterationsPerKey; j++) {
                testData.add(new CacheRequest(i, i, "name"));
            }
        }

        // 2. Warmup (important!)
        runPhase(testData.subList(0, testData.size() / 10), usePgCache);

        // 3. Measurement phase
        long start = System.nanoTime();

        runPhase(testData, usePgCache);

        long end = System.nanoTime();

        long durationMs = (end - start) / 1_000_000;

        double opsPerSec = (testData.size() * 1_000.0) / durationMs;

        return new TestResult(durationMs, opsPerSec);
    }

    private void runPhase(List<CacheRequest> data,
                          boolean usePgCache) {

        int threads = 32;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {

            CountDownLatch latch = new CountDownLatch(data.size());
            List<CacheRequest> shuffled = new ArrayList<>(data);
            Collections.shuffle(shuffled, new Random(42));

            for (CacheRequest req : data) {
                executor.submit(() -> {
                    try {
                        if (usePgCache) {
                            externalClient.getDataUsingPgCache(req);
                        } else {
                            externalClient.getDataUsingRedisCache(req);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                log.error("interrupter", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
