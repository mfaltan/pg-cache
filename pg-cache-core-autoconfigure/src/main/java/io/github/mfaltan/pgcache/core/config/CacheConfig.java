package io.github.mfaltan.pgcache.core.config;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.core.PgCacheManager;
import io.github.mfaltan.pgcache.core.cache.PgCacheFactory;
import io.github.mfaltan.pgcache.core.cache.PgCacheFactoryImpl;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.executor.PgCacheExecutorHolder;
import io.github.mfaltan.pgcache.core.executor.PgCacheTaskDecorator;
import io.github.mfaltan.pgcache.core.serializer.CacheValueSerializer;
import io.github.mfaltan.pgcache.core.store.PgCacheStore;
import io.github.mfaltan.pgcache.core.store.PgCacheStoreImpl;
import io.github.mfaltan.pgcache.core.util.CurrentDateTimeProvider;
import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import io.github.mfaltan.pgcache.resilience.NoOpCacheResilienceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableConfigurationProperties(PgCacheConfigurationProperties.class)
@Slf4j
public class CacheConfig {

    @Bean
    @ConditionalOnMissingBean
    CurrentDateTimeProvider currentDateTimeProvider() {
        log.info(Constants.MARKER, "Initializing pg cache default time provider");
        return LocalDateTime::now;
    }



    @Bean
    @ConditionalOnMissingBean
    PgCacheStore pgCacheStore(PgCacheConfigurationProperties properties,
                              @Qualifier("pgCacheAdminDataSource") DataSource adminDataSource,
                              @Qualifier("pgCacheUserReadDataSource") DataSource userReadDataSource,
                              @Qualifier("pgCacheUserWriteDataSource") DataSource userWriteDataSource,
                              CurrentDateTimeProvider currentDateTimeProvider) {

        log.info(Constants.MARKER, "Initializing pg cache store");
        return new PgCacheStoreImpl(userReadDataSource, userWriteDataSource, adminDataSource, currentDateTimeProvider, properties.getTableName());
    }

    @Bean
    @ConditionalOnMissingBean
    PgCacheFactory pgCacheFactory(CacheExecutorHolder cacheExecutorHolder,
                                  PgCacheStore pgCacheStore,
                                  List<CacheValueSerializer> serializers,
                                  PgCacheConfigurationProperties properties) {
        return new PgCacheFactoryImpl(pgCacheStore, cacheExecutorHolder, serializers, properties);
    }

    @Bean("pgCacheManager")
    @ConditionalOnMissingBean(name = "pgCacheManager")
    CacheManager pgCacheManager(PgCacheFactory pgCacheFactory,
                                CacheResilienceFactory cacheResilienceFactory,
                                PgCacheConfigurationProperties properties) {

        log.info(Constants.MARKER, "Initializing pg cache manager");

        return new PgCacheManager(pgCacheFactory, cacheResilienceFactory, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    CacheResilienceFactory cacheResilienceFactory() {
        log.info(Constants.MARKER, "Initializing pg cache noOp resilience factory");
        return new NoOpCacheResilienceFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "pgCacheTaskDecorator")
    TaskDecorator pgCacheTaskDecorator() {
        log.info(Constants.MARKER, "Initializing pg cache task decorator");
        return new PgCacheTaskDecorator();
    }

    @Bean
    @ConditionalOnMissingBean
    CacheExecutorHolder executorHolder(PgCacheConfigurationProperties properties,
                                       @Qualifier("pgCacheTaskDecorator") TaskDecorator taskDecorator) {
        log.info(Constants.MARKER, "Initializing pg cache executor holder");
        return new PgCacheExecutorHolder(properties.getAsync(), taskDecorator);
    }
}
