package io.github.mfaltan.pgcache.resilience.config;

import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import io.github.mfaltan.pgcache.resilience.PgCacheResilienceFactory;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.github.mfaltan.pgcache.common.Constants.MARKER;

@Configuration
@Slf4j
public class CacheResilienceConfig {


    @Bean
    CacheResilienceFactory pgCacheResilienceFactory(CircuitBreakerRegistry circuitBreakerRegistry,
                                                    @Value("${pg-cache.resilience.prefix:pg-cache}") String prefix) {
        log.info(MARKER, "Pg-cache resilience enabled, overriding noOp resilience");
        return new PgCacheResilienceFactory(circuitBreakerRegistry, prefix);
    }
}
