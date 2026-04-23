package io.github.mfaltan.pgcache.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.core.PgCacheInterceptor;
import io.github.mfaltan.pgcache.core.PgCacheManager;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.executor.PgCacheExecutorHolder;
import io.github.mfaltan.pgcache.core.executor.PgCacheTaskDecorator;
import io.github.mfaltan.pgcache.core.serializer.CacheValueSerializer;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializer;
import io.github.mfaltan.pgcache.core.store.CacheStoreFactory;
import io.github.mfaltan.pgcache.core.store.PgCacheStoreFactory;
import io.github.mfaltan.pgcache.core.util.CurrentDateTimeProvider;
import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import io.github.mfaltan.pgcache.resilience.NoOpCacheResilienceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AbstractCachingConfiguration;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.core.task.TaskDecorator;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableConfigurationProperties(PgCacheConfigurationProperties.class)
@Slf4j
public class CacheConfig extends AbstractCachingConfiguration {

    @Bean
    CurrentDateTimeProvider currentDateTimeProvider() {
        log.info(Constants.MARKER, "Initializing pg cache default time provider");
        return LocalDateTime::now;
    }

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

    @Bean
    CacheStoreFactory storeFactory(PgCacheConfigurationProperties properties,
                                   @Qualifier("pgCacheAdminDataSource") DataSource adminDataSource,
                                   @Qualifier("pgCacheUserReadDataSource") DataSource userReadDataSource,
                                   @Qualifier("pgCacheUserWriteDataSource") DataSource userWriteDataSource,
                                   CurrentDateTimeProvider currentDateTimeProvider) {

        log.info(Constants.MARKER, "Initializing pg cache store factory");
        return PgCacheStoreFactory.builder()
                                  .adminDataSource(adminDataSource)
                                  .userReadDataSource(userReadDataSource)
                                  .userWriteDataSource(userWriteDataSource)
                                  .tableName(properties.getTableName())
                                  .defaultTtlSeconds(properties.getDefaultTtlSeconds())
                                  .timeProvider(currentDateTimeProvider)
                                  .build();
    }

    @Bean
    CacheValueSerializer valueSerializer(ObjectMapper objectMapper) {
        log.info(Constants.MARKER, "Initializing default pg cache Jackson serializer / deserializer");
        return new PgCacheSerializer(objectMapper);
    }

    @Bean("pgCacheManager")
    CacheManager cacheManager(CacheExecutorHolder cacheExecutorHolder,
                              CacheStoreFactory cacheStoreFactory,
                              CacheResilienceFactory cacheResilienceFactory,
                              List<CacheValueSerializer> serializers,
                              PgCacheConfigurationProperties properties) {

        log.info(Constants.MARKER, "Initializing pg cache manager");

        return new PgCacheManager(cacheExecutorHolder, cacheStoreFactory, cacheResilienceFactory, properties, serializers);
    }

    @Bean("pgCacheInterceptor")
    @Primary
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    CacheInterceptor cacheInterceptor(CacheOperationSource cacheOperationSource) {
        log.info(Constants.MARKER, "Initializing pg cache interceptor");

        CacheInterceptor interceptor = new PgCacheInterceptor();
        interceptor.configure(this.errorHandler, this.keyGenerator, this.cacheResolver, this.cacheManager);
        interceptor.setCacheOperationSource(cacheOperationSource);
        return interceptor;
    }

    @Bean
    CacheResilienceFactory cacheResilienceFactory() {
        log.info(Constants.MARKER, "Initializing pg cache noOp resilience factory");
        return new NoOpCacheResilienceFactory();
    }

    @Bean
    TaskDecorator pgCacheTaskDecorator() {
        log.info(Constants.MARKER, "Initializing pg cache task decorator");
        return new PgCacheTaskDecorator();
    }

    @Bean
    CacheExecutorHolder executorHolder(PgCacheConfigurationProperties properties,
                                       @Qualifier("pgCacheTaskDecorator") TaskDecorator taskDecorator) {
        log.info(Constants.MARKER, "Initializing pg cache executor holder");
        return new PgCacheExecutorHolder(properties.getAsync(), taskDecorator);
    }
}
