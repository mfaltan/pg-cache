package io.github.mfaltan.pgcache.perftest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.core.PgCacheManager;
import io.github.mfaltan.pgcache.core.cache.PgCacheFactory;
import io.github.mfaltan.pgcache.core.config.properties.PgCacheConfigurationProperties;
import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class PgCacheConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    @Primary
    PgCacheManager pgCacheManager(PgCacheFactory pgCacheFactory,
                                  CacheResilienceFactory cacheResilienceFactory,
                                  PgCacheConfigurationProperties properties) {

        return new PgCacheManager(pgCacheFactory, cacheResilienceFactory, properties);
    }
}