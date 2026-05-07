package io.github.mfaltan.pgcache.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.cache.PgCacheFactoryImpl;
import io.github.mfaltan.pgcache.core.domain.KeyEntry;
import io.github.mfaltan.pgcache.core.executor.PgCacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.PgCacheGeneralSerializerImpl;
import io.github.mfaltan.pgcache.core.store.PgCacheStoreImpl;
import io.github.mfaltan.pgcache.resilience.NoOpCacheResilienceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static java.util.Collections.emptyMap;
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

        var store = new PgCacheStoreImpl(dataSource, dataSource, dataSource, LocalDateTime::now, "cache_data");
        store.init();
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {

            var rs = stmt.executeQuery("""
                        SELECT tablename
                        FROM pg_tables
                        WHERE tablename = 'cache_data'
                    """);

            assertThat(rs.next()).isTrue();
        }

        var cache = getCache(store);
        var type = new TypeReference<SomeValueClass>() {
        }.getType();
        var someKey = KeyEntry.builder()
                              .rawKey("someKey")
                              .type(type)
                              .build();

        var someValue = new SomeValueClass("f1", 22L);
        assert cache != null;
        cache.put(someKey, someValue);
        var storedValue = cache.get(someKey);
        assert storedValue != null;
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
        assert storedValue != null;
        assertThat(storedValue.get()).isNull();
    }

    private static Cache getCache(PgCacheStoreImpl store) {
        var generalSerializer = new PgCacheGeneralSerializerImpl(new ObjectMapper());
        var pgCacheProperties = new PgCacheProperties();
        pgCacheProperties.setCleanupEnabled(false);
        pgCacheProperties.setDefaultTtlSeconds(10);
        var cacheResilienceFactory = new NoOpCacheResilienceFactory();
        var executorHolder = new PgCacheExecutorHolder(pgCacheProperties.getAsync(), (s) -> (s));
        var cacheFactory = new PgCacheFactoryImpl(store, executorHolder, generalSerializer, emptyMap(), pgCacheProperties);
        var cacheManager = new PgCacheManager(cacheFactory, cacheResilienceFactory, pgCacheProperties);

        return cacheManager.getCache("cache1");
    }

    private record SomeValueClass(String field1, Long field2) {
    }
}