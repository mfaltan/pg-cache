package io.github.mfaltan.pgcache.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.core.CurrentDateTimeProvider;
import io.github.mfaltan.pgcache.core.JacksonSerializer;
import io.github.mfaltan.pgcache.core.PgCacheInterceptor;
import io.github.mfaltan.pgcache.core.PgCacheManager;
import io.github.mfaltan.pgcache.core.PgStoreFactory;
import io.github.mfaltan.pgcache.core.StoreFactory;
import io.github.mfaltan.pgcache.core.StoreProperties;
import io.github.mfaltan.pgcache.core.ValueSerializer;
import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import io.github.mfaltan.pgcache.resilience.NoOpCacheResilienceFactory;
import org.postgresql.ds.PGSimpleDataSource;
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

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(PgCacheProperties.class)
public class CacheConfig extends AbstractCachingConfiguration {

    @Bean
    CurrentDateTimeProvider currentDateTimeProvider() {
        return LocalDateTime::now;
    }

    @Bean
    DataSource pgCacheUserReadDataSource(PgCacheProperties properties) {
        return HikariDataSourceFactory.create(properties.getUserReadDataSource());
    }

    @Bean
    DataSource pgCacheUserWriteDataSource(PgCacheProperties properties) {
        return HikariDataSourceFactory.create(properties.getUserWriteDataSource());
    }

    @Bean(name = "pgCacheAdminDataSource")
    DataSource pgCacheAdminDataSource(PgCacheProperties properties) {
        PgCacheProperties.DataSourceProperties props = properties.getAdminDatasource();
        PGSimpleDataSource ds = new org.postgresql.ds.PGSimpleDataSource();
        ds.setURL(props.getUrl());
        ds.setUser(props.getUsername());
        ds.setPassword(props.getPassword());
        return ds;
    }

    @Bean
    StoreFactory storeFactory(PgCacheProperties properties,
                              @Qualifier("pgCacheAdminDataSource") DataSource adminDataSource,
                              @Qualifier("pgCacheUserReadDataSource") DataSource userReadDataSource,
                              @Qualifier("pgCacheUserWriteDataSource") DataSource userWriteDataSource,
                              CurrentDateTimeProvider currentDateTimeProvider) {

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
        return new JacksonSerializer(objectMapper);
    }

    @Bean("pgCacheManager")
    CacheManager cacheManager(StoreFactory storeFactory,
                              ValueSerializer valueSerializer,
                              CacheResilienceFactory cacheResilienceFactory,
                              PgCacheProperties properties) {
        Map<String, StoreProperties> caches = new HashMap<>(properties.getCaches());
        return PgCacheManager.builder()
                             .storeFactory(storeFactory)
                             .serializer(valueSerializer)
                             .cacheResilienceFactory(cacheResilienceFactory)
                             .storesProperties(caches)
                             .cleanupEnabled(properties.isCleanupEnabled())
                             .cleanupEnabledSupplier(() -> true)
                             .cleanupLimit(properties.getCleanupLimit())
                             .build();
    }

    @Bean("pgCacheInterceptor")
    @Primary
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    CacheInterceptor cacheInterceptor(CacheOperationSource cacheOperationSource) {
        CacheInterceptor interceptor = new PgCacheInterceptor();
        interceptor.configure(this.errorHandler, this.keyGenerator, this.cacheResolver, this.cacheManager);
        interceptor.setCacheOperationSource(cacheOperationSource);
        return interceptor;
    }

    @Bean
    CacheResilienceFactory cacheResilienceFactory() {
        return new NoOpCacheResilienceFactory();
    }
}
