package io.github.mfaltan.pgcache.config;

import io.github.mfaltan.pgcache.core.StoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "pg-cache")
@Getter
@Setter
public class PgCacheProperties {

    /**
     * Enables pg-cache integration
     */
    private boolean enabled = false;

    /**
     * Table name for cache
     */
    private String tableName = "cache_data";

    /**
     * Default TTL
     */
    private int defaultTtlSeconds = 30 * 60;

    /**
     * Default cleanup setup
     */
    private boolean cleanupEnabled = false;
    private int cleanupLimit = 1000;

    /**
     * Per-cache configuration
     */
    private Map<String, CacheProperties> caches = new HashMap<>();


    /**
     * Admin datasource (DDL, schema management)
     */
    private DataSourceProperties adminDatasource = new DataSourceProperties();
    private final DataSourceProperties userReadDataSource = new DataSourceProperties();
    private final DataSourceProperties userWriteDataSource = new DataSourceProperties();

    @Getter
    @Setter
    public static class CacheProperties implements StoreProperties {
        /**
         * TTL in seconds (optional, falls back to default)
         */
        private Integer ttlSeconds;
    }

    @Getter
    @Setter
    public static class DataSourceProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";
        private int maximumPoolSize = 10;
        private long connectionTimeoutMs = 30000;
        private long idleTimeoutMs = 600000;
        private long maxLifetimeMs = 1800000;
    }
}