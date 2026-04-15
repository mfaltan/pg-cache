package io.github.mfaltan.pgcache.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PgCacheManagerIT {

    private static final int TTL_SECONDS_1 = 30;
    private static final int TTL_SECONDS_2 = 45;
    private static final String CACHE_1 = "cache1";
    private static final String CACHE_2 = "cache2";
    private static final String SOME_KEY = "someKey";
    private static final String SOME_VALUE = "someValue";

    private PgCacheManager pgCacheManager;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RamStoreFactory storeFactory = new RamStoreFactory();
    private final ValueSerializer serializer = new JacksonSerializer(mapper);
    private final Map<String, StoreProperties> storesProperties = new HashMap<>();

    @BeforeEach
    void init() {
        storesProperties.clear();
        storesProperties.put(CACHE_1, createStoreProperties(TTL_SECONDS_1));
        storesProperties.put(CACHE_2, createStoreProperties(TTL_SECONDS_2));

        pgCacheManager = new PgCacheManager(storeFactory, serializer, storesProperties);
    }

    @Test
    void should_return_null_because_nothing_is_cached() {

        //when
        var cache1 = pgCacheManager.getCache(CACHE_1);
        assert cache1 != null;
        var someKey = KeyEntry.builder()
                              .rawKey(SOME_KEY)
                              .build();
        var actual = cache1.get(someKey);

        //then
        assertThat(actual).isNull();
    }

    @ParameterizedTest
    @MethodSource("simpleValuesToBeCached")
    void should_cache_simple_value_and_then_return(Object valueToBeCached, Type type) {

        //when
        var someKey = KeyEntry.builder()
                              .rawKey(valueToBeCached)
                              .type(type)
                              .build();

        var cache1 = pgCacheManager.getCache(CACHE_1);
        assert cache1 != null;
        cache1.put(someKey, valueToBeCached);
        var valueWrapper = cache1.get(someKey);
        assert valueWrapper != null;
        var actual = valueWrapper.get();

        //then
        assertThat(actual).isEqualTo(valueToBeCached);
    }

    @ParameterizedTest
    @MethodSource("simpleValuesToBeCached")
    void should_cache_simple_value_and_then_try_to_get_it_from_incorrect_cache(Object valueToBeCached, Type type) {

        //when
        var someKey = KeyEntry.builder()
                              .rawKey(valueToBeCached)
                              .type(type)
                              .build();

        var cache1 = pgCacheManager.getCache(CACHE_1);
        assert cache1 != null;
        cache1.put(someKey, valueToBeCached);
        var cache2 = pgCacheManager.getCache(CACHE_2);
        assert cache2 != null;
        var valueWrapper = cache2.get(someKey);

        //then
        assertThat(valueWrapper).isNull();
    }

    @ParameterizedTest
    @MethodSource("valuesToBeCached")
    void should_cache_object_and_then_return_typed(Object valueToBeCached, Class<?> clazz, Type type) {

        //when
        var someKey = KeyEntry.builder()
                              .rawKey(valueToBeCached)
                              .type(type)
                              .build();
        var cache1 = pgCacheManager.getCache(CACHE_1);
        assert cache1 != null;
        cache1.put(someKey, valueToBeCached);
        var actual = cache1.get(someKey, clazz);

        //then
        assertThat(actual).isEqualTo(valueToBeCached);
    }

    @ParameterizedTest
    @MethodSource("valuesToBeCached")
    void should_cache_object_then_evict_then_return_null(Object valueToBeCached, Class<?> clazz, Type type) {

        //when
        var someKey = KeyEntry.builder()
                              .rawKey(valueToBeCached)
                              .type(type)
                              .build();
        var cache1 = pgCacheManager.getCache(CACHE_1);
        assert cache1 != null;
        cache1.put(someKey, valueToBeCached);
        cache1.evict(someKey);
        var actual = cache1.get(someKey, clazz);

        //then
        assertThat(actual).isNull();
    }

    static Stream<Arguments> simpleValuesToBeCached() {
        return Stream.of(
                Arguments.of(SOME_VALUE, new TypeReference<String>() {
                }.getType()),
                Arguments.of(1, new TypeReference<Integer>() {
                }.getType()),
                Arguments.of(null, new TypeReference<Void>() {
                }.getType()));
    }

    static Stream<Arguments> valuesToBeCached() {
        return Stream.of(
                Arguments.of(SOME_VALUE, String.class, new TypeReference<String>() {
                }.getType()),
                Arguments.of(someValueObject(), ValueClass.class, new TypeReference<ValueClass>() {
                }.getType()),
                Arguments.of(null, ValueClass.class, new TypeReference<ValueClass>() {
                }.getType()));
    }

    private static ValueClass someValueObject() {
        return new ValueClass("Peter", 22L);
    }

    private static StoreProperties createStoreProperties(Integer ttlSeconds) {
        return StoreProp.builder()
                        .ttlSeconds(ttlSeconds)
                        .build();
    }


    private record ValueClass(String name, Long age) {
    }

    @Getter
    @Builder
    private static class StoreProp implements StoreProperties {
        Integer ttlSeconds;
    }
}