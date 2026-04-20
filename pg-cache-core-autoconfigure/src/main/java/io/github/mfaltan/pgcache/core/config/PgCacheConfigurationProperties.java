package io.github.mfaltan.pgcache.core.config;

import io.github.mfaltan.pgcache.common.PgCacheProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pg-cache")
@Getter
@Setter
public class PgCacheConfigurationProperties extends PgCacheProperties {

}