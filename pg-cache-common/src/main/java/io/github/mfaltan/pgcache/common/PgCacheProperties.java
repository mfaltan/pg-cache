package io.github.mfaltan.pgcache.common;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class PgCacheProperties {

    /**
     * Enables pg-cache integration
     */
    private boolean enabled = false;

    /**
     * PgCache by default use custom serializing & deserializing, which is fully automatic.
     * However, it overrides default CacheInterceptor provided by Spring framework.
     * If this is not acceptable, there is an option to use custom serializers instead
     */
    private boolean customSerializers = false;

    /**
     * Table name for cache
     */
    private String tableName = "cache_data";

    /**
     * Default TTL
     */
    private int defaultTtlSeconds = 30 * 60;

    /**
     * Async get timeout in seconds
     */
    private int asyncGetTimeout = 5;

    /**
     * Async get with loader timeout in seconds
     */
    private int asyncGetWithLoaderTimeout = 30;

    /**
     * Default cleanup setup
     */
    private boolean cleanupEnabled = false;
    private int cleanupLimit = 1000;

    /**
     * Per-cache configuration
     */
    private final Map<String, CacheProperties> caches = new HashMap<>();

    private final AsyncProperties async = new AsyncProperties();

    /**
     * Scheduled jobs will create unique UUID and put it to MDC context under this key
     */
    private String traceIdKey = "traceId";

    private final DataSourceProperties dataSource = new DataSourceProperties();
    private final DataSourceProperties writeDataSource = new DataSourceProperties();
    private final DataSourceProperties adminDatasource = new DataSourceProperties();

    @Getter
    @Setter
    public static class CacheProperties implements StoreProperties {

        private boolean disabled;
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

    @Getter
    @Setter
    public static class AsyncProperties {
        private boolean enabled = false;
        private int writeThreads = 10;
        private int writeQueue = 100;
        private int clearThreads = 10;
        private int clearQueue = 100;
    }
}