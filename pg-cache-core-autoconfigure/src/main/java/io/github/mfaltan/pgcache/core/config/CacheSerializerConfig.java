package io.github.mfaltan.pgcache.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.core.PgCacheInterceptor;
import io.github.mfaltan.pgcache.core.serializer.CacheValueSerializer;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.AbstractCachingConfiguration;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;

@Configuration
@ConditionalOnProperty(
        prefix = "pg-cache",
        name = "use-universal-serializer",
        havingValue = "true"
)
@Slf4j
public class CacheSerializerConfig extends AbstractCachingConfiguration {

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
    CacheValueSerializer valueSerializer(ObjectMapper objectMapper) {
        log.info(Constants.MARKER, "Initializing default pg cache Jackson serializer / deserializer");
        return new PgCacheSerializer(objectMapper);
    }
}
