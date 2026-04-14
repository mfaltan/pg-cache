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

    /**
     * Table name for cache
     */
    private String tableName = "cache_data";

    /**
     * Admin datasource (DDL, schema management)
     */
    private DataSourceProperties adminDatasource = new DataSourceProperties();
    private final DataSourceProperties userDataSource = new DataSourceProperties();

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