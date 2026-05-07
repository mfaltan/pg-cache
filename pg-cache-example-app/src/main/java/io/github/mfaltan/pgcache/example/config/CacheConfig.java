package io.github.mfaltan.pgcache.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerConfiguration;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerString;
import io.github.mfaltan.pgcache.example.Constants;
import io.github.mfaltan.pgcache.example.serializer.PgCacheSerializerRequest;
import io.github.mfaltan.pgcache.example.serializer.PgCacheSerializerListUuid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CacheConfig {

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
    Map<String, PgCacheSerializerConfiguration> serializerConfigurations(ObjectMapper objectMapper) {

        Map<String, PgCacheSerializerConfiguration> ret = new HashMap<>();

        var stringSerializer = new PgCacheSerializerString(objectMapper);

        //default serializer will just fail, that is intention for test purposes
        var defaultConfig = PgCacheSerializerConfiguration.builder()
                                                          .keySerializer(stringSerializer)
                                                          .valueSerializer(stringSerializer)
                                                          .build();

        var customKeySerializer = new PgCacheSerializerRequest(objectMapper);
        var customValueSerializer = new PgCacheSerializerListUuid(objectMapper);

        var cache1Config = PgCacheSerializerConfiguration.builder()
                                                          .keySerializer(stringSerializer)
                                                          .valueSerializer(customValueSerializer)
                                                          .build();

        var cache2Config = PgCacheSerializerConfiguration.builder()
                                                         .keySerializer(customKeySerializer)
                                                         .valueSerializer(customValueSerializer)
                                                         .build();

        ret.put(null, defaultConfig);
        ret.put(Constants.CACHE_1, cache1Config);
        ret.put(Constants.CACHE_2, cache2Config);

        return ret;
    }
}