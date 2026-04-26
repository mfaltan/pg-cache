package io.github.mfaltan.pgcache.core.config;

import io.github.mfaltan.pgcache.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(PgCacheConfigurationProperties.class)
@Slf4j
public class CacheDataSourceConfig {

    @Bean
    DataSource pgCacheUserReadDataSource(PgCacheConfigurationProperties properties) {
        log.info(Constants.MARKER, "Initializing pg cache user read data source");
        return HikariDataSourceFactory.create(properties.getUserReadDataSource());
    }

    @Bean
    DataSource pgCacheUserWriteDataSource(PgCacheConfigurationProperties properties) {
        log.info(Constants.MARKER, "Initializing pg cache user write data source");
        return HikariDataSourceFactory.create(properties.getUserWriteDataSource());
    }

    @Bean
    DataSource pgCacheAdminDataSource(PgCacheConfigurationProperties properties) {
        log.info(Constants.MARKER, "Initializing pg cache admin data source");
        return HikariDataSourceFactory.create(properties.getAdminDatasource());
    }
}
