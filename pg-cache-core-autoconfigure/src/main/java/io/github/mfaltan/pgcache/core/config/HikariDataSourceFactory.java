package io.github.mfaltan.pgcache.core.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.mfaltan.pgcache.common.PgCacheProperties.DataSourceProperties;

import javax.sql.DataSource;

public class HikariDataSourceFactory {

    private HikariDataSourceFactory() {

    }

    public static DataSource create(DataSourceProperties props) {

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(props.getUrl());
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());

        config.setMaximumPoolSize(props.getMaximumPoolSize());
        config.setConnectionTimeout(props.getConnectionTimeoutMs());
        config.setIdleTimeout(props.getIdleTimeoutMs());
        config.setMaxLifetime(props.getMaxLifetimeMs());

        return new HikariDataSource(config);
    }

}
