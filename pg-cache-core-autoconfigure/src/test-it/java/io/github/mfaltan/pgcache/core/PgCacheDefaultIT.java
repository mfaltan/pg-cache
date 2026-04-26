package io.github.mfaltan.pgcache.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = TestBootstrap.class)
public class PgCacheDefaultIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("pgcache")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {

        registry.add("pg-cache.enabled", () -> true);
        registry.add("pg-cache.use-universal-serializer", () -> true);
        registry.add("pg-cache.admin-datasource.url", postgres::getJdbcUrl);
        registry.add("pg-cache.admin-datasource.username", postgres::getUsername);
        registry.add("pg-cache.admin-datasource.password", postgres::getPassword);

        registry.add("pg-cache.user-read-datasource.url", postgres::getJdbcUrl);
        registry.add("pg-cache.user-read-datasource.username", postgres::getUsername);
        registry.add("pg-cache.user-read-datasource.password", postgres::getPassword);

        registry.add("pg-cache.user-write-datasource.url", postgres::getJdbcUrl);
        registry.add("pg-cache.user-write-datasource.username", postgres::getUsername);
        registry.add("pg-cache.user-write-datasource.password", postgres::getPassword);

        registry.add("pg-cache.default-ttl-seconds", () -> 60);
    }

    @Autowired
    private ExpensiveService service;

    @Test
    void should_cache_result_after_first_call() {

        String v1 = service.compute("A");
        String v2 = service.compute("A");
        String v3 = service.compute("A");

        assertThat(service.getInvocationCount()).isEqualTo(1);

        String v4 = service.compute("B");

        assertThat(v1).isEqualTo(v2);
        assertThat(v2).isEqualTo(v3);
        assertThat(v4).isEqualTo("value-B-2");
        assertThat(service.getInvocationCount()).isEqualTo(2);

        String v5 = service.evict("A");
        assertThat(v5).isEqualTo("evict-A-3");

        String v6 = service.compute("A");
        assertThat(v6).isEqualTo("value-A-4");

        service.evictAll();

        String v7 = service.compute("A");
        assertThat(v7).isEqualTo("value-A-5");
    }


}
