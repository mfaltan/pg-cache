package io.github.mfaltan.pgcache.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.resilience.NoOpCacheResilienceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class SimplePgOperationsIT {

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
                                    .userReadDataSource(dataSource)
                                    .userWriteDataSource(dataSource)
                                    .tableName("cache_data")
                                    .timeProvider(LocalDateTime::now)
                                    .defaultTtlSeconds(20)
                                    .build();

        factory.init();

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {

            var rs = stmt.executeQuery("""
                        SELECT tablename
                        FROM pg_tables
                        WHERE tablename = 'cache_data'
                    """);

            assertThat(rs.next()).isTrue();
        }

        var valueSerializer = new JacksonSerializer(new ObjectMapper());
        var storesProperties = new HashMap<String, StoreProperties>();
        var cacheResilienceFactory = new NoOpCacheResilienceFactory();
        var cacheManager = new PgCacheManager(factory, valueSerializer, cacheResilienceFactory, storesProperties, false, 10);

        var cache = cacheManager.getCache("cache1");
        var type = new TypeReference<SomeValueClass>() {
        }.getType();
        var someKey = KeyEntry.builder()
                              .rawKey("someKey")
                              .type(type)
                              .build();

        var someValue = new SomeValueClass("f1", 22L);

        cache.put(someKey, someValue);
        var storedValue = cache.get(someKey);
        assertThat(storedValue.get()).isEqualTo(someValue);

        cache.evict(someKey);
        storedValue = cache.get(someKey);
        assertThat(storedValue).isNull();

        cache.put(someKey, someValue);
        cache.clear();
        storedValue = cache.get(someKey);
        assertThat(storedValue).isNull();

        cache.put(someKey, null);
        storedValue = cache.get(someKey);
        assertThat(storedValue.get()).isNull();
    }

    private record SomeValueClass(String field1, Long field2) {
    }
}