package io.github.mfaltan.pgcache.resilience.config;

import io.github.mfaltan.pgcache.resilience.CacheResilienceFactory;
import io.github.mfaltan.pgcache.resilience.PgCacheResilienceFactory;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Slf4j
public class CacheResilienceConfig {


    @Bean
    @Primary
    CacheResilienceFactory pgCacheResilienceFactory(CircuitBreakerRegistry circuitBreakerRegistry,
                                                    @Value("${pg-cache.resilience.prefix:pg-cache}") String prefix) {
        log.info("Pg-cache resilience enabled");
        return new PgCacheResilienceFactory(circuitBreakerRegistry, prefix);
    }
}
