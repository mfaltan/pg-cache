package io.github.mfaltan.pgcache.perftest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.perftest.serializer.RedisResultSerializer;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory,
                                               ObjectMapper objectMapper) {

        RedisResultSerializer valueSerializer = new RedisResultSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                                                                .entryTtl(Duration.ofMinutes(10))
                                                                .disableCachingNullValues()
                                                                .serializeKeysWith(
                                                                        RedisSerializationContext.SerializationPair
                                                                                .fromSerializer(new StringRedisSerializer())
                                                                )
                                                                .serializeValuesWith(
                                                                        RedisSerializationContext.SerializationPair
                                                                                .fromSerializer(valueSerializer)
                                                                )
                                                                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(config)
                                .build();
    }

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceCustomizer() {
        return builder -> builder
                .commandTimeout(Duration.ofMillis(2000))
                .shutdownTimeout(Duration.ZERO);
    }
}