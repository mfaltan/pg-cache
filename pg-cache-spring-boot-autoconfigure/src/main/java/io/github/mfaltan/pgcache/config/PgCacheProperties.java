package io.github.mfaltan.pgcache.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pg-cache")
@Getter
@Setter
public class PgCacheProperties {

    /**
     * Enables pg-cache integration
     */
    private boolean enabled = false;
}