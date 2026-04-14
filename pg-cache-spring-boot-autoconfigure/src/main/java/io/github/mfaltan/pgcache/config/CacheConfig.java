package io.github.mfaltan.pgcache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.core.*;
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

@Configuration
@EnableConfigurationProperties(PgCacheProperties.class)
public class CacheConfig extends AbstractCachingConfiguration {

    @Bean(name = "pgCacheAdminDataSource")
    public DataSource pgCacheAdminDataSource(PgCacheProperties properties) {
        PgCacheProperties.DataSourceProperties props = properties.getAdminDatasource();
        PGSimpleDataSource ds = new org.postgresql.ds.PGSimpleDataSource();
        ds.setURL(props.getUrl());
        ds.setUser(props.getUsername());
        ds.setPassword(props.getPassword());
        return ds;
    }

    @Bean
    StoreFactory storeFactory(PgCacheProperties properties,
                              @Qualifier("pgCacheAdminDataSource") DataSource adminDataSource){
        return PgStoreFactory.builder()
                             .adminDataSource(adminDataSource)
                             .tableName(properties.getTableName())
                             .build();
    }

    @Bean
    @ConditionalOnMissingBean
    ValueSerializer valueSerializer(ObjectMapper objectMapper) {
        return new JacksonSerializer(objectMapper);
    }

    @Bean("pgCacheManager")
    CacheManager cacheManager(StoreFactory storeFactory, ValueSerializer valueSerializer) {
        return new PgCacheManager(storeFactory, valueSerializer);
    }

    @Bean("pgCacheInterceptor")
    @Primary
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public CacheInterceptor cacheInterceptor(CacheOperationSource cacheOperationSource) {
        CacheInterceptor interceptor = new PgCacheInterceptor();
        interceptor.configure(this.errorHandler, this.keyGenerator, this.cacheResolver, this.cacheManager);
        interceptor.setCacheOperationSource(cacheOperationSource);
        return interceptor;
    }
}
