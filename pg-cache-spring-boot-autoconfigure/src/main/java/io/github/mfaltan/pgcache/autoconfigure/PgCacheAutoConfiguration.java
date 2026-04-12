package io.github.mfaltan.pgcache.autoconfigure;

import io.github.mfaltan.pgcache.config.CacheConfig;
import io.github.mfaltan.pgcache.config.PgCacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

/**
 * Auto-configures pg-cache when included via starter.
 */
@AutoConfiguration
@EnableCaching
@EnableConfigurationProperties(PgCacheProperties.class)
@ConditionalOnProperty(
        prefix = "pg-cache",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass(CacheConfig.class)
@Import(CacheConfig.class)
@Slf4j
public class PgCacheAutoConfiguration {

    public PgCacheAutoConfiguration() {
        log.info("pg-cache enabled");
    }
}