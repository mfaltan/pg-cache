package io.github.mfaltan.pgcache.core;

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
public class PgStoreFactory extends RamStoreFactory implements StoreFactory {
    private final DataSource adminDataSource;
    private final String tableName;

    @PostConstruct
    public void init() throws Exception {
        createTableIfNotExists();
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
