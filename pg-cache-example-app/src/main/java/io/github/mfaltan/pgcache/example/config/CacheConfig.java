package io.github.mfaltan.pgcache.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerConfiguration;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerString;
import io.github.mfaltan.pgcache.example.Constants;
import io.github.mfaltan.pgcache.example.serializer.PgCacheSerializerListUuid;
import io.github.mfaltan.pgcache.example.serializer.PgCacheSerializerRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CacheConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    @Primary
    CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(Constants.CACHE_3);
    }


    @ConditionalOnProperty(
            prefix = "pg-cache",
            name = "custom-serializers",
            havingValue = "true"
    )
    @Bean
    PgCacheSerializerConfiguration pgCacheSerializerConfiguration(ObjectMapper objectMapper) {

        var stringSerializer = new PgCacheSerializerString(objectMapper);
        var customKeySerializer = new PgCacheSerializerRequest(objectMapper);
        var customValueSerializer = new PgCacheSerializerListUuid(objectMapper);

        return PgCacheSerializerConfiguration.builder()
                                             .serializerConfiguration(null, stringSerializer, stringSerializer)
                                             .serializerConfiguration(Constants.CACHE_1, stringSerializer, customValueSerializer)
                                             .serializerConfiguration(Constants.CACHE_2, customKeySerializer, customValueSerializer)
                                             .build();
    }
}