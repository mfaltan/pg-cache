package io.github.mfaltan.pgcache.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.core.CurrentDateTimeProvider;
import io.github.mfaltan.pgcache.core.ExecutorHolder;
import io.github.mfaltan.pgcache.core.JacksonSerializer;
import io.github.mfaltan.pgcache.core.PgCacheInterceptor;
import io.github.mfaltan.pgcache.core.PgCacheManager;
import io.github.mfaltan.pgcache.core.PgExecutorHolder;
import io.github.mfaltan.pgcache.core.PgStoreFactory;
import io.github.mfaltan.pgcache.core.PgCacheTaskDecorator;
import io.github.mfaltan.pgcache.core.StoreFactory;
import io.github.mfaltan.pgcache.core.ValueSerializer;
import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import io.github.mfaltan.pgcache.resilience.NoOpCacheResilienceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    StoreFactory storeFactory(PgCacheConfigurationProperties properties,
                              @Qualifier("pgCacheAdminDataSource") DataSource adminDataSource,
                              @Qualifier("pgCacheUserReadDataSource") DataSource userReadDataSource,
                              @Qualifier("pgCacheUserWriteDataSource") DataSource userWriteDataSource,
                              CurrentDateTimeProvider currentDateTimeProvider) {

        log.info(Constants.MARKER, "Initializing pg cache store factory");
        return PgStoreFactory.builder()
                             .adminDataSource(adminDataSource)
                             .userReadDataSource(userReadDataSource)
                             .userWriteDataSource(userWriteDataSource)
                             .tableName(properties.getTableName())
                             .defaultTtlSeconds(properties.getDefaultTtlSeconds())
                             .timeProvider(currentDateTimeProvider)
                             .build();
    }

    @Bean
    @ConditionalOnMissingBean
    ValueSerializer valueSerializer(ObjectMapper objectMapper) {
        log.info(Constants.MARKER, "Initializing default pg cache Jackson serializer / deserializer");
        return new JacksonSerializer(objectMapper);
    }

    @Bean("pgCacheManager")
    CacheManager cacheManager(ExecutorHolder executorHolder,
                              StoreFactory storeFactory,
                              ValueSerializer valueSerializer,
                              CacheResilienceFactory cacheResilienceFactory,
                              PgCacheConfigurationProperties properties) {

        log.info(Constants.MARKER, "Initializing pg cache manager");
        return PgCacheManager.builder()
                             .executorHolder(executorHolder)
                             .storeFactory(storeFactory)
                             .serializer(valueSerializer)
                             .cacheResilienceFactory(cacheResilienceFactory)
                             .properties(properties)
                             .build();
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
    ExecutorHolder executorHolder(PgCacheConfigurationProperties properties,
                                  @Qualifier("pgCacheTaskDecorator") TaskDecorator taskDecorator) {
        log.info(Constants.MARKER, "Initializing pg cache executor holder");
        return new PgExecutorHolder(properties.getAsync(), taskDecorator);
    }
}
