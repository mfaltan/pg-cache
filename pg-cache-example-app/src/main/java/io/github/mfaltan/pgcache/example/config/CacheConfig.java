package io.github.mfaltan.pgcache.example.config;

import io.github.mfaltan.pgcache.example.Constants;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CacheConfig {

    @Bean
    @Primary
    CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(Constants.CACHE_3);
    }
}