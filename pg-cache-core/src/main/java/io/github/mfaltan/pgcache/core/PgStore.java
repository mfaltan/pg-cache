package io.github.mfaltan.pgcache.core;


import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.core.exception.PgCacheStoreException;
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
public class PgStore implements Store {

    private final DataSource readDataSource;
    private final DataSource writeDataSource;
    private final DataSource adminDataSource;
    private final CurrentDateTimeProvider timeProvider;
    private final String tableName;
    private final String cacheName;

    private final int ttlSeconds;

    @Override
    public void put(Long key, CacheEntry entry) {
        log.debug(Constants.MARKER, "Put value for key [{}] to cache [{}]", key, cacheName);

        String sql = """
                INSERT INTO %s (name, key, raw_key, value, expires_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (name, key)
                DO UPDATE SET
                    raw_key = EXCLUDED.raw_key,
                    value = EXCLUDED.value,
                    expires_at = EXCLUDED.expires_at
                """.formatted(tableName);

        try (Connection conn = writeDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cacheName);
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
    public CacheEntry get(Long key) {
        log.debug(Constants.MARKER, "Get key [{}] from cache [{}]", key, cacheName);

        String sql = """
                SELECT raw_key, value, expires_at
                FROM %s
                WHERE name = ? AND key = ? AND expires_at > ?
                """.formatted(tableName);

        try (var conn = readDataSource.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, cacheName);
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
    public void evictExpired(int limit) {
        log.debug(Constants.MARKER, "Evicting expired entries from cache [{}]", cacheName);

        var sql = """
                DELETE FROM %s
                WHERE ctid IN (
                    SELECT ctid
                    FROM %s
                    WHERE expires_at < ?
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                """.formatted(tableName, tableName);
        var now = timeProvider.now();

        try {
            try (var conn = writeDataSource.getConnection();
                 var ps = conn.prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(now));
                ps.setInt(2, limit);
                ps.executeUpdate();

                log.info(MARKER, "Part of the expired entries for cache [{}] was evicted", cacheName);
            }
        } catch (SQLException e) {
            throw new PgCacheStoreException("Limited evict failed", e);
        }
    }

    @Override
    public void remove(Long key) {
        log.debug(Constants.MARKER, "Removing key [{}] from cache [{}]", key, cacheName);

        String sql = """
                DELETE FROM %s
                WHERE name = ? AND key = ?
                """.formatted(tableName);

        try (Connection conn = writeDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cacheName);
            ps.setLong(2, key);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new PgCacheStoreException("Failed to REMOVE cache entry", e);
        }
    }

    @Override
    public void clear() {
        log.info(MARKER, "Truncating whole partition [{}]", tableName);
        String sql = "TRUNCATE TABLE " + tableName;

        try (Connection conn = adminDataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);

        } catch (SQLException e) {
            throw new PgCacheStoreException("Failed to CLEAR cache", e);
        }
    }
}
