package io.github.mfaltan.pgcache.core.store;

import io.github.mfaltan.pgcache.core.util.CurrentDateTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgCacheStoreFactoryTest {

    private static final String TABLE_NAME = "cache_data";
    private static final String CACHE_NAME = "cache1";

    private PgCacheStoreFactory factory;

    @Mock
    DataSource adminDataSource, userReadDataStore, userWriteDataStore;

    @Mock
    CurrentDateTimeProvider timeProvider;

    @Mock
    Connection connection;

    @Mock
    Statement statement;

    @BeforeEach
    void init() {
        factory = new PgCacheStoreFactory(adminDataSource, userReadDataStore, userWriteDataStore, TABLE_NAME, timeProvider);
    }

    @Test
    void should_return_expected_store() throws SQLException {
        // GIVEN
        var expected = createStore();
        when(adminDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        // WHEN
        var actual = factory.initializeStore(CACHE_NAME);

        // THEN
        assertThat(actual).isEqualTo(expected);
        verify(statement, times(1)).execute(contains("CREATE UNLOGGED TABLE IF NOT EXISTS cache_data_cache1"));
    }

    @Test
    void should_create_table_and_index() throws SQLException {
        // GIVEN
        when(adminDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        // WHEN
        factory.init();

        // THEN
        verify(statement, times(1)).execute(contains("CREATE TABLE IF NOT EXISTS cache_data"));
        verify(statement, times(1)).execute(contains("CREATE INDEX IF NOT EXISTS idx_cache_data_expires_at"));

        verify(statement, times(1)).close();
        verify(connection, times(1)).close();
    }

    @Test
    void should_throw_exception_when_connection_fails() throws Exception {
        // GIVEN
        var e = new RuntimeException();
        when(adminDataSource.getConnection()).thenThrow(e);

        // WHEN + THEN
        assertThatThrownBy(factory::init).isEqualTo(e);
        verifyNoMoreInteractions(connection, statement);
    }

    private PgCacheStore createStore() {
        return PgCacheStore.builder()
                           .readDataSource(userReadDataStore)
                           .writeDataSource(userWriteDataStore)
                           .adminDataSource(adminDataSource)
                           .timeProvider(timeProvider)
                           .cacheName(CACHE_NAME)
                           .tableName(TABLE_NAME + "_" + CACHE_NAME)
                           .build();
    }
}