package io.github.mfaltan.pgcache.core.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableCaching
@EnableScheduling
@Import({CacheConfig.class, CacheSerializerConfig.class, CacheDataSourceConfig.class})
public class CacheCoreConfig {
}
