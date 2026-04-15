package io.github.mfaltan.pgcache.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PgStoreFactoryTest {

    private static final String TABLE_NAME = "cache_data";
    private static final String CACHE_NAME = "cache1";
    private static final int DEFAULT_TTL = 30;

    private  PgStoreFactory factory;

    @Mock
    DataSource adminDataSource, userDataStore;

    @Mock
    CurrentDateTimeProvider timeProvider;

    @Mock
    Connection connection;

    @Mock
    Statement statement;

    @Mock
    StoreProperties storeProperties;

    @BeforeEach
    void init() {
        factory = new PgStoreFactory(adminDataSource, userDataStore, TABLE_NAME, timeProvider, DEFAULT_TTL);
    }

    @Test
    void should_return_expected_store_with_default_ttl() {
        // GIVEN
        var expected = createStore(DEFAULT_TTL);
        when(storeProperties.getTtlSeconds()).thenReturn(null);
        // WHEN
        var actual = factory.initializeStore(CACHE_NAME, storeProperties);

        // THEN
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_return_expected_store_with_default_ttl_again() {
        // GIVEN
        var expected = createStore(DEFAULT_TTL);

        // WHEN
        var actual = factory.initializeStore(CACHE_NAME, null);

        // THEN
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_return_expected_store_with_custom_ttl() {
        // GIVEN
        var expectedTtl = 55;
        var expected = createStore(expectedTtl);
        when(storeProperties.getTtlSeconds()).thenReturn(expectedTtl);
        // WHEN
        var actual = factory.initializeStore(CACHE_NAME, storeProperties);

        // THEN
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_create_table_and_index() throws Exception {
        // GIVEN
        when(adminDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        // WHEN
        factory.init();

        // THEN
        verify(statement, times(1)).execute(contains("CREATE UNLOGGED TABLE IF NOT EXISTS cache_data"));
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

    private PgStore createStore(int expectedTtl) {
        return PgStore.builder()
                      .dataSource(userDataStore)
                      .timeProvider(timeProvider)
                      .cacheName(CACHE_NAME)
                      .tableName(TABLE_NAME)
                      .ttlSeconds(expectedTtl)
                      .build();
    }
}