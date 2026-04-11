package io.github.mfaltan.pgcache.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PgCacheManagerIT {

    private static final String CACHE_1 = "cache1";
    private static final String CACHE_2 = "cache2";
    private static final String SOME_KEY = "someKey";
    private static final String SOME_VALUE = "someValue";

    private PgCacheManager pgCacheManager;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ValueSerializer serializer = new JacksonSerializer(mapper);

    @BeforeEach
    void init() {
        pgCacheManager = new PgCacheManager(serializer);
    }

    @Test
    void should_return_null_because_nothing_is_cached() {

        //when
        var cache1 = pgCacheManager.getCache(CACHE_1);
        assert cache1 != null;
        var actual = cache1.get(SOME_KEY);

        //then
        assertThat(actual).isNull();
    }

    @ParameterizedTest
    @MethodSource("simpleValuesToBeCached")
    void should_cache_simple_value_and_then_return(Object valueToBeCached) {

        //when
        var cache1 = pgCacheManager.getCache(CACHE_1);
        assert cache1 != null;
        cache1.put(SOME_KEY, valueToBeCached);
        var valueWrapper = cache1.get(SOME_KEY);
        assert valueWrapper != null;
        var actual = valueWrapper.get();

        //then
        assertThat(actual).isEqualTo(valueToBeCached);
    }

    @ParameterizedTest
    @MethodSource("simpleValuesToBeCached")
    void should_cache_simple_value_and_then_try_to_get_it_from_incorrect_cache(Object valueToBeCached) {

        //when
        var cache1 = pgCacheManager.getCache(CACHE_1);
        assert cache1 != null;
        cache1.put(SOME_KEY, valueToBeCached);
        var cache2 = pgCacheManager.getCache(CACHE_2);
        assert cache2 != null;
        var valueWrapper = cache2.get(SOME_KEY);

        //then
        assertThat(valueWrapper).isNull();
    }

    @ParameterizedTest
    @MethodSource("valuesToBeCached")
    void should_cache_object_and_then_return_typed(Object valueToBeCached, Class<?> clazz) {

        //when
        var cache1 = pgCacheManager.getCache(CACHE_1);
        assert cache1 != null;
        cache1.put(SOME_KEY, valueToBeCached);
        var actual = cache1.get(SOME_KEY, clazz);

        //then
        assertThat(actual).isEqualTo(valueToBeCached);
    }

    static Stream<Object> simpleValuesToBeCached() {
        return Stream.of(SOME_VALUE, 1, null);
    }

    static Stream<Arguments> valuesToBeCached() {
        return Stream.of(
                Arguments.of(SOME_VALUE, String.class),
                Arguments.of(someValueObject(), ValueClass.class),
                Arguments.of(null, ValueClass.class));
    }

    private static ValueClass someValueObject() {
        return new ValueClass("Peter", 22L);
    }

    private record ValueClass(String name, Long age) {
    }
}