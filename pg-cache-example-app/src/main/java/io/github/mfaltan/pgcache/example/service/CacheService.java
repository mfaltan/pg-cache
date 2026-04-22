package io.github.mfaltan.pgcache.example.service;

import io.github.mfaltan.pgcache.example.Constants;
import io.github.mfaltan.pgcache.example.client.ExternalClient;
import io.github.mfaltan.pgcache.example.dto.CacheRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static io.github.mfaltan.pgcache.common.Constants.MARKER;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final ExternalClient client;

    public List<UUID> getCache(int age, String name) {
        return client.getData(age, name);
    }

    @CacheEvict(value = Constants.CACHE_1, key = "#p0 + '-' + #p1")
    public void evictCache(int age, String name) {
        log.info(MARKER, "Age [{}] and name [{}] removed from cache1", age, name);
    }

    public List<UUID> getCache(CacheRequest cacheRequest) {
        return client.getData(cacheRequest);
    }

    @CacheEvict(value = Constants.CACHE_2)
    public void evictCache(CacheRequest cacheRequest) {
        log.info(MARKER, "[{}] removed from cache2", cacheRequest);

    }
}
