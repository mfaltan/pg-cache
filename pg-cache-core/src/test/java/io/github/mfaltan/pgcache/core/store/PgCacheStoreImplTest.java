package io.github.mfaltan.pgcache.core.store;

import io.github.mfaltan.pgcache.core.domain.CacheEntry;
import io.github.mfaltan.pgcache.core.exception.PgCacheStoreException;
import io.github.mfaltan.pgcache.core.util.CurrentDateTimeProvider;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgCacheStoreImplTest {

    private static final String TABLE = "pgcache";
    private static final String CACHE = "users";
    private static final Long KEY = 123L;

    private static final byte[] RAW_KEY = "raw".getBytes();
    private static final byte[] VALUE = "value".getBytes();
    private static final LocalDateTime NOW = LocalDateTime.of(2025, 1, 1, 12, 0);

    private PgCacheStoreImpl store;

    @Mock
    private DataSource readDataSource, writeDataSource, adminDataSource;
    @Mock
    private CurrentDateTimeProvider timeProvider;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private Statement statement;
    @Mock
    private ResultSet resultSet;
    @Mock
    private CacheEntry cacheEntry;

    @BeforeEach
    void setUp() {
        store = new PgCacheStoreImpl(
                readDataSource,
                writeDataSource,
                adminDataSource,
                timeProvider,
                TABLE
        );
    }

    @AfterEach
    void verifyNoUnexpectedInteractions() {
        verifyNoMoreInteractions(
                readDataSource,
                writeDataSource,
                adminDataSource,
                timeProvider,
                connection,
                preparedStatement,
                statement,
                resultSet,
                cacheEntry
        );
    }

    @Test
    void should_init_store() throws Exception {
        // GIVEN
        var tableSql = """
                CREATE TABLE IF NOT EXISTS %s (
                    name TEXT NOT NULL,
                    key BIGINT NOT NULL,
                    raw_key BYTEA NOT NULL,
                    value BYTEA NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    PRIMARY KEY (name, key)
                )  PARTITION BY LIST (name)
                """.formatted(TABLE);

        var indexSql = """
                CREATE INDEX IF NOT EXISTS idx_%s_expires_at
                ON %s (expires_at)
                """.formatted(TABLE, TABLE);

        when(adminDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        //WHEN
        store.init();

        //THEN
        verify(statement).execute(tableSql);
        verify(statement).execute(indexSql);
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void should_init_partition() throws Exception {
        //GIVEN
        var partitionSql = """
                CREATE UNLOGGED TABLE IF NOT EXISTS %s
                PARTITION OF %s
                FOR VALUES IN ('%s')
                """.formatted(getPartitionName(), TABLE, CACHE);

        when(adminDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        //WHEN
        store.initCache(CACHE);

        //THEN
        verify(statement).execute(partitionSql);
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void should_wrap_exception_when_initializing_partition() throws Exception {
        //GIVEN
        var e = new SQLException();
        when(adminDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenThrow(e);

        //WHEN + THEN
        assertThatThrownBy(() -> store.initCache(CACHE))
                .isInstanceOf(PgCacheStoreException.class)
                .hasCause(e);

        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void should_put_entry() throws Exception {
        //GIVEN
        var sql = """
                INSERT INTO %s (name, key, raw_key, value, expires_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (name, key)
                DO UPDATE SET
                    raw_key = EXCLUDED.raw_key,
                    value = EXCLUDED.value,
                    expires_at = EXCLUDED.expires_at
                """.formatted(getPartitionName());

        when(timeProvider.now()).thenReturn(NOW);
        when(writeDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(cacheEntry.normalizedKey()).thenReturn(RAW_KEY);
        when(cacheEntry.value()).thenReturn(VALUE);

        //WHEN
        store.put(KEY, cacheEntry, 60, CACHE);

        //THEN
        verify(preparedStatement).setString(1, CACHE);
        verify(preparedStatement).setLong(2, KEY);
        verify(preparedStatement).setBytes(3, RAW_KEY);
        verify(preparedStatement).setBytes(4, VALUE);
        verify(preparedStatement).setTimestamp(5, Timestamp.valueOf(NOW.plusSeconds(60)));
        verify(preparedStatement).executeUpdate();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void should_wrap_exception_when_putting() throws Exception {
        //GIVEN
        var e = new SQLException();
        when(writeDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(e);

        //WHEN + THEN
        assertThatThrownBy(() -> store.put(KEY, cacheEntry, 60, CACHE))
                .isInstanceOf(PgCacheStoreException.class)
                .hasCause(e);
        verify(connection).close();
    }

    @Test
    void should_return_null_when_not_found() throws Exception {
        //GIVEN
        var sql = """
                SELECT raw_key, value, expires_at
                FROM %s
                WHERE name = ? AND key = ? AND expires_at > ?
                """.formatted(getPartitionName());

        when(readDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(timeProvider.now()).thenReturn(NOW);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        //WHEN
        var actual = store.get(KEY, CACHE);

        //THEN
        assertThat(actual).isNull();
        verify(preparedStatement).setString(1, CACHE);
        verify(preparedStatement).setLong(2, KEY);
        verify(preparedStatement).setTimestamp(3, Timestamp.valueOf(NOW));
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }


    @Test
    void should_return_value_when_not_found() throws Exception {
        //GIVEN
        var sql = """
                SELECT raw_key, value, expires_at
                FROM %s
                WHERE name = ? AND key = ? AND expires_at > ?
                """.formatted(getPartitionName());

        when(readDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(timeProvider.now()).thenReturn(NOW);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBytes("raw_key")).thenReturn(RAW_KEY);
        when(resultSet.getBytes("value")).thenReturn(VALUE);

        var expected = CacheEntry.builder()
                                 .normalizedKey(RAW_KEY)
                                 .value(VALUE)
                                 .build();

        //WHEN
        var actual = store.get(KEY, CACHE);

        //THEN
        assertThat(actual).isEqualTo(expected);
        verify(preparedStatement).setString(1, CACHE);
        verify(preparedStatement).setLong(2, KEY);
        verify(preparedStatement).setTimestamp(3, Timestamp.valueOf(NOW));
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void should_wrap_exception_when_getting() throws Exception {
        //GIVEN
        var e = new SQLException();
        when(readDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(timeProvider.now()).thenReturn(NOW);
        when(preparedStatement.executeQuery()).thenThrow(e);

        //WHEN + THEN
        assertThatThrownBy(() -> store.get(KEY, CACHE))
                .isInstanceOf(PgCacheStoreException.class)
                .hasCause(e);

        verify(preparedStatement).setString(1, CACHE);
        verify(preparedStatement).setLong(2, KEY);
        verify(preparedStatement).setTimestamp(3, Timestamp.valueOf(NOW));
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void should_evict_expired() throws Exception {
        //GIVEN
        var sql = """
                DELETE FROM %s
                WHERE ctid IN (
                    SELECT ctid
                    FROM %s
                    WHERE expires_at < ?
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                """.formatted(getPartitionName(), getPartitionName());

        when(writeDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(timeProvider.now()).thenReturn(NOW);

        //WHEN
        store.evictExpired(10, CACHE);

        //THEN
        verify(preparedStatement).setTimestamp(1, Timestamp.valueOf(NOW));
        verify(preparedStatement).setInt(2, 10);
        verify(preparedStatement).executeUpdate();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void should_wrap_exception_when_evicting_expired() throws Exception {
        //GIVEN
        var e = new SQLException();
        when(writeDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(timeProvider.now()).thenReturn(NOW);
        when(preparedStatement.executeUpdate()).thenThrow(e);

        //WHEN + THEN
        assertThatThrownBy(() -> store.evictExpired(11, CACHE))
                .isInstanceOf(PgCacheStoreException.class)
                .hasCause(e);

        verify(preparedStatement).setTimestamp(1, Timestamp.valueOf(NOW));
        verify(preparedStatement).setInt(2, 11);
        verify(preparedStatement).close();
        verify(connection).close();
    }


    @Test
    void should_remove_entry() throws Exception {
        //GIVEN
        var sql = """
                DELETE FROM %s
                WHERE name = ? AND key = ?
                """.formatted(getPartitionName());

        when(writeDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);

        //WHEN
        store.remove(KEY, CACHE);

        //THEN
        verify(preparedStatement).setString(1, CACHE);
        verify(preparedStatement).setLong(2, KEY);
        verify(preparedStatement).executeUpdate();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void should_wrap_exception_when_removing_entry() throws Exception {
        //GIVEN
        var e = new SQLException();
        when(writeDataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(e);

        //WHEN + THEN
        assertThatThrownBy(() -> store.remove(KEY, CACHE))
                .isInstanceOf(PgCacheStoreException.class)
                .hasCause(e);

        verify(preparedStatement).setString(1, CACHE);
        verify(preparedStatement).setLong(2, KEY);
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void should_clear() throws Exception {
        //GIVEN
        var sql = "TRUNCATE TABLE " + getPartitionName();

        when(adminDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        //WHEN
        store.clear(CACHE);

        //THEN
        verify(statement).execute(sql);
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void should_wrap_exception_when_clearing() throws Exception {
        //GIVEN
        var e = new SQLException();
        when(adminDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenThrow(e);

        //WHEN + THEN
        assertThatThrownBy(() -> store.clear(CACHE))
                .isInstanceOf(PgCacheStoreException.class)
                .hasCause(e);

        verify(statement).close();
        verify(connection).close();
    }

    private static @NonNull String getPartitionName() {
        return TABLE + "_" + CACHE;
    }
}