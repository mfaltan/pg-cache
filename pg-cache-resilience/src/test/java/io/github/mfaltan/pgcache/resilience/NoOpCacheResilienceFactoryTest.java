package io.github.mfaltan.pgcache.resilience;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpCacheResilienceFactoryTest {

    private static final String CACHE_NAME = "cache-name";

    private final NoCacheResilienceFactory factory = new NoCacheResilienceFactory();

    @Test
    void should_create_new_no_cache_resilience_instance() {
        // WHEN
        var result = factory.create(CACHE_NAME);

        // THEN
        assertThat(result).isInstanceOf(NoOpCacheResilience.class);
    }

    @Test
    void should_create_new_instance_each_time() {
        // WHEN
        var first = factory.create(CACHE_NAME);
        var second = factory.create(CACHE_NAME);

        // THEN
        assertThat(first).isNotSameAs(second);
    }
}
