package io.github.mfaltan.pgcache.core;

import io.github.mfaltan.pgcache.core.exception.PgCacheStoreException;
import io.github.mfaltan.pgcache.core.exception.PgStoreFactoryException;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@RequiredArgsConstructor
@Builder
@Slf4j
public class PgStoreFactory implements StoreFactory {
    private final DataSource adminDataSource;
    private final DataSource userReadDataSource;
    private final DataSource userWriteDataSource;
    private final String tableName;
    private final CurrentDateTimeProvider timeProvider;
    private final int defaultTtlSeconds;

    @PostConstruct
    public void init() {
        try {
            createTableIfNotExists();
        } catch (SQLException e) {
            throw new PgStoreFactoryException("Error when initializing pg store", e);
        }
    }

    @Override
    public Store initializeStore(String name, StoreProperties storeProperties) {
        var ttlSeconds = storeProperties != null ? storeProperties.getTtlSeconds() : null;

        return PgStore.builder()
                      .readDataSource(userReadDataSource)
                      .writeDataSource(userWriteDataSource)
                      .timeProvider(timeProvider)
                      .cacheName(name)
                      .tableName(tableName)
                      .ttlSeconds(ttlSeconds != null ? ttlSeconds : defaultTtlSeconds)
                      .build();
    }

    private void createTableIfNotExists() throws SQLException {
        log.info("Initializing pg cache table");

        String tableSql = """
                CREATE UNLOGGED TABLE IF NOT EXISTS %s (
                    name TEXT NOT NULL,
                    key BIGINT NOT NULL,
                    raw_key BYTEA NOT NULL,
                    value BYTEA NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    PRIMARY KEY (name, key)
                )
                """.formatted(tableName);

        String indexSql = """
                CREATE INDEX IF NOT EXISTS idx_%s_expires_at
                ON %s (expires_at)
                """.formatted(tableName, tableName);

        try (Connection conn = adminDataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. create table
            stmt.execute(tableSql);

            // 2. create index
            stmt.execute(indexSql);
        }
    }
}
