package io.github.mfaltan.pgcache.core.config;

import io.github.mfaltan.pgcache.core.config.datasource.DataSourceHolder;
import io.github.mfaltan.pgcache.core.config.properties.PgCacheConfigurationProperties;
import io.github.mfaltan.pgcache.core.config.properties.SpringDataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PgCacheConfigurationProperties.class, SpringDataSourceProperties.class})
@Slf4j
public class CacheDataSourceConfig {

    @Bean
    DataSourceHolder dataSourceHolder(PgCacheConfigurationProperties pgCacheProperties, SpringDataSourceProperties springDataSourceProperties) {
        return new DataSourceHolder(pgCacheProperties, springDataSourceProperties);
    }
}
