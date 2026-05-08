package io.github.mfaltan.pgcache.core.config.datasource;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.common.PgCacheProperties.DataSourceProperties;
import io.github.mfaltan.pgcache.core.config.properties.PgCacheConfigurationProperties;
import io.github.mfaltan.pgcache.core.config.properties.SpringDataSourceProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

@RequiredArgsConstructor
@Slf4j
public class DataSourceHolder {

    private final PgCacheConfigurationProperties properties;
    private final SpringDataSourceProperties springDsProperties;

    @Getter
    private DataSource readDataSource;

    @Getter
    private DataSource writeDataSource;

    @Getter
    private DataSource adminDataSource;

    @PostConstruct
    public void init() {
        var rds = properties.getDataSource();
        if (StringUtils.hasLength(rds.getUrl())) {
            log.info(Constants.MARKER, "Initializing custom pg-cache-datasource");
            readDataSource = HikariDataSourceFactory.create(rds);
        } else {
            log.info(Constants.MARKER, "Using copy of spring datasource as pg-cache-datasource");
            DataSourceProperties readProp = createSpringDataSourceProperties(rds);
            readDataSource = HikariDataSourceFactory.create(readProp);
        }

        var wds = properties.getWriteDataSource();
        if (StringUtils.hasLength(wds.getUsername())) {
            log.info(Constants.MARKER, "Initializing custom pg-cache-write-datasource");
            writeDataSource = HikariDataSourceFactory.create(wds);
        } else {
            log.info(Constants.MARKER, "Using pg-cache-datasource also as pg-cache-write-datasource");
            writeDataSource = readDataSource;
        }

        var ads = properties.getAdminDatasource();
        if (StringUtils.hasLength(ads.getUsername())) {
            log.info(Constants.MARKER, "Initializing custom pg-cache-admin-datasource");
            adminDataSource = HikariDataSourceFactory.create(ads);
        } else {
            log.info(Constants.MARKER, "Using pg-cache-write-datasource also as pg-cache-admin-datasource");
            adminDataSource = writeDataSource;
        }
    }

    private @NonNull DataSourceProperties createSpringDataSourceProperties(DataSourceProperties rds) {
        DataSourceProperties readProp = new DataSourceProperties();
        readProp.setUrl(springDsProperties.getUrl());
        readProp.setUsername(springDsProperties.getUsername());
        readProp.setPassword(springDsProperties.getPassword());
        readProp.setDriverClassName(springDsProperties.getDriverClassName());

        readProp.setMaximumPoolSize(rds.getMaximumPoolSize());
        readProp.setIdleTimeoutMs(rds.getIdleTimeoutMs());
        readProp.setMaxLifetimeMs(rds.getMaxLifetimeMs());
        readProp.setConnectionTimeoutMs(rds.getConnectionTimeoutMs());
        return readProp;
    }
}
