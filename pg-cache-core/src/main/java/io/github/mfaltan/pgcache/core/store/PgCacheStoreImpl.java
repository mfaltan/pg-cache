package io.github.mfaltan.pgcache.core.store;


import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.core.domain.CacheEntry;
import io.github.mfaltan.pgcache.core.exception.PgCacheStoreException;
import io.github.mfaltan.pgcache.core.util.CurrentDateTimeProvider;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import static io.github.mfaltan.pgcache.common.Constants.MARKER;

@Slf4j
@RequiredArgsConstructor
@Builder
@EqualsAndHashCode
public class PgCacheStoreImpl implements PgCacheStore {

    private final DataSource readDataSource;
    private final DataSource writeDataSource;
    private final DataSource adminDataSource;
    private final CurrentDateTimeProvider timeProvider;
    private final String tableName;

    @PostConstruct
    public void init() throws SQLException {
        initMainTable();
    }

    @Override
    public void initCache(String name) {
        var partitionName = getPartitionName(name);
        initializePartition(name, partitionName);
    }


    @Override
    public void put(Long key, CacheEntry entry, int ttlSeconds, String name) {
        log.debug(Constants.MARKER, "Put value for key [{}] to cache [{}]", key, name);

        var partitionName = getPartitionName(name);
        var sql = """
                INSERT INTO %s (name, key, raw_key, value, expires_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (name, key)
                DO UPDATE SET
                    raw_key = EXCLUDED.raw_key,
                    value = EXCLUDED.value,
                    expires_at = EXCLUDED.expires_at
                """.formatted(partitionName);

        try (Connection conn = writeDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setLong(2, key);
            ps.setBytes(3, entry.normalizedKey());
            ps.setBytes(4, entry.value());
            ps.setTimestamp(5, Timestamp.valueOf(timeProvider.now().plusSeconds(ttlSeconds)));
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new PgCacheStoreException("Failed to PUT cache entry", e);
        }
    }

    @Override
    public CacheEntry get(Long key, String name) {
        log.debug(Constants.MARKER, "Get key [{}] from cache [{}]", key, name);

        var partitionName = getPartitionName(name);
        var sql = """
                SELECT raw_key, value, expires_at
                FROM %s
                WHERE name = ? AND key = ? AND expires_at > ?
                """.formatted(partitionName);

        try (var conn = readDataSource.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setLong(2, key);
            ps.setTimestamp(3, Timestamp.valueOf(timeProvider.now()));

            try (var rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                return CacheEntry.builder()
                                 .normalizedKey(rs.getBytes("raw_key"))
                                 .value(rs.getBytes("value"))
                                 .build();
            }

        } catch (SQLException e) {
            throw new PgCacheStoreException("GET failed", e);
        }
    }

    @Override
    public void evictExpired(int limit, String name) {
        log.debug(Constants.MARKER, "Evicting expired entries from cache [{}]", name);

        var partitionName = getPartitionName(name);
        var sql = """
                DELETE FROM %s
                WHERE ctid IN (
                    SELECT ctid
                    FROM %s
                    WHERE expires_at < ?
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                """.formatted(partitionName, partitionName);
        var now = timeProvider.now();

        try {
            try (var conn = writeDataSource.getConnection();
                 var ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(now));
                ps.setInt(2, limit);
                ps.executeUpdate();

                log.info(MARKER, "Part of the expired entries for cache [{}] was evicted", name);
            }
        } catch (SQLException e) {
            throw new PgCacheStoreException("Limited evict failed", e);
        }
    }

    @Override
    public void remove(Long key, String name) {
        log.debug(Constants.MARKER, "Removing key [{}] from cache [{}]", key, name);

        var partitionName = getPartitionName(name);
        var sql = """
                DELETE FROM %s
                WHERE name = ? AND key = ?
                """.formatted(partitionName);

        try (Connection conn = writeDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setLong(2, key);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new PgCacheStoreException("Failed to REMOVE cache entry", e);
        }
    }

    @Override
    public void clear(String name) {
        var partitionName = getPartitionName(name);

        log.info(MARKER, "Truncating whole partition [{}]", partitionName);

        var sql = "TRUNCATE TABLE " + partitionName;

        try (Connection conn = adminDataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);

        } catch (SQLException e) {
            throw new PgCacheStoreException("Failed to CLEAR cache", e);
        }
    }

    private void initMainTable() throws SQLException {
        log.info(MARKER, "Initializing pg cache table");

        String tableSql = """
                CREATE TABLE IF NOT EXISTS %s (
                    name TEXT NOT NULL,
                    key BIGINT NOT NULL,
                    raw_key BYTEA NOT NULL,
                    value BYTEA NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    PRIMARY KEY (name, key)
                )  PARTITION BY LIST (name)
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

    private void initializePartition(String name, String partitionName) {
        log.info(MARKER, "Initializing pg cache partition [{}]", partitionName);

        var partitionSql = """
                CREATE UNLOGGED TABLE IF NOT EXISTS %s
                PARTITION OF %s
                FOR VALUES IN ('%s')
                """.formatted(partitionName, tableName, name);

        try (Connection conn = adminDataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(partitionSql);
        } catch (SQLException e) {
            throw new PgCacheStoreException("Error when creating partition", e);
        }
    }

    private String getPartitionName(String name) {
        return tableName + "_" + name;
    }
}
