package io.github.mfaltan.pgcache.example.config;

import io.github.mfaltan.pgcache.example.Constants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "pg-cache.enabled",havingValue = "false", matchIfMissing = true)
public class CacheConfig {

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(Constants.CACHE_1, Constants.CACHE_2);
    }
}