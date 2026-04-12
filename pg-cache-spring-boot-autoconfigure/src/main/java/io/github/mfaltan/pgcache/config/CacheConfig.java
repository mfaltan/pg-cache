package io.github.mfaltan.pgcache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.core.JacksonSerializer;
import io.github.mfaltan.pgcache.core.PgCacheInterceptor;
import io.github.mfaltan.pgcache.core.PgCacheManager;
import io.github.mfaltan.pgcache.core.ValueSerializer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AbstractCachingConfiguration;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;

@Configuration
public class CacheConfig extends AbstractCachingConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    ValueSerializer valueSerializer(ObjectMapper objectMapper) {
        return new JacksonSerializer(objectMapper);
    }

    @Bean("pgCacheManager")
    CacheManager cacheManager(ValueSerializer valueSerializer) {
        return new PgCacheManager(valueSerializer);
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
