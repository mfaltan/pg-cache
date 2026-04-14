package io.github.mfaltan.pgcache.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PgStoreFactoryTest {

    private static final String NAME = "cache_data";

    private  PgStoreFactory factory;

    @Mock
    DataSource adminDataSource;

    @Mock
    Connection connection;

    @Mock
    Statement statement;

    @BeforeEach
    void init() {
        factory = new PgStoreFactory(adminDataSource, NAME);
    }
    @Test
    void shouldCreateTableAndIndex() throws Exception {
        // given
        when(adminDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        // when
        factory.init();

        // then
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
}