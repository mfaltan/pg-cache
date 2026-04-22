package io.github.mfaltan.pgcache.core.autoconfigure;

import io.github.mfaltan.pgcache.core.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import static io.github.mfaltan.pgcache.common.Constants.MARKER;

/**
 * Auto-configures pg-cache when included via starter.
 */
@AutoConfiguration
@EnableCaching
@EnableScheduling
@ConditionalOnProperty(
        prefix = "pg-cache",
        name = "enabled",
        havingValue = "true"
)
@Import(CacheConfig.class)
@Slf4j
public class PgCacheCoreAutoConfiguration {

    public PgCacheCoreAutoConfiguration() {
        log.info(MARKER, "Pg-cache core enabled");
    }
}