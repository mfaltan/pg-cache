package io.github.mfaltan.pgcache.resilience.autoconfigure;

import io.github.mfaltan.pgcache.resilience.config.CacheResilienceConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(CacheResilienceConfig.class)
public class PgCacheResilienceAutoConfiguration {
}