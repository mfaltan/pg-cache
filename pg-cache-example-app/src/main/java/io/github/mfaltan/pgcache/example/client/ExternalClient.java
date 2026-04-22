package io.github.mfaltan.pgcache.example.client;

import io.github.mfaltan.pgcache.example.Constants;
import io.github.mfaltan.pgcache.example.dto.CacheRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static io.github.mfaltan.pgcache.common.Constants.MARKER;

@Component
@Slf4j
public class ExternalClient {

    @Cacheable(value = Constants.CACHE_1, key = "#p0 + '-' + #p1")
    public List<UUID> getData(int age, String name) {
        log.info(MARKER, "Getting new data for age [{}] and name [{}]", age, name);
        int count = ThreadLocalRandom.current().nextInt(1, 11);

        return IntStream.range(0, count)
                        .mapToObj(i -> UUID.randomUUID())
                        .toList();
    }

    @Cacheable(value = Constants.CACHE_2)
    public List<UUID> getData(CacheRequest cacheRequest) {
        log.info(MARKER, "Getting new data for [{}]", cacheRequest);

        int count = ThreadLocalRandom.current().nextInt(1, 11);

        return IntStream.range(0, count)
                        .mapToObj(i -> UUID.randomUUID())
                        .toList();
    }
}