package io.github.mfaltan.pgcache.resilience.autoconfigure;

import io.github.mfaltan.pgcache.resilience.config.CacheResilienceConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(beforeName = "io.github.mfaltan.pgcache.core.autoconfigure.PgCacheCoreAutoConfiguration")
@Import(CacheResilienceConfig.class)
public class PgCacheResilienceAutoConfiguration {
}