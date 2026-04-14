package io.github.mfaltan.pgcache.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

@Testcontainers
class PgStoreFactoryIT {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("pgcache")
                    .withUsername("test")
                    .withPassword("test");
    private DataSource dataSource;

    @BeforeEach
    void setup() {
        var ds = new org.postgresql.ds.PGSimpleDataSource();
        ds.setURL(postgres.getJdbcUrl());
        ds.setUser(postgres.getUsername());
        ds.setPassword(postgres.getPassword());
        this.dataSource = ds;
    }

    @Test
    void should_create_table_and_index() throws Exception {

        var factory = PgStoreFactory.builder()
                                    .adminDataSource(dataSource)
                                    .tableName("cache_data")
                                    .build();

        factory.init();

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {

            var rs = stmt.executeQuery("""
                        SELECT tablename
                        FROM pg_tables
                        WHERE tablename = 'cache_data'
                    """);

            Assertions.assertThat(rs.next()).isTrue();
        }
    }
}