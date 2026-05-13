package io.github.mfaltan.pgcache.core.serializer;

import io.github.mfaltan.pgcache.core.exception.PgCacheSerializerConfigurationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PgCacheSerializerConfigurationTest {

    private static final String CACHE_NAME = "users";

    private PgCacheSerializerConfiguration configuration;
    private Map<String, PgCacheSerializerPair> serializerPairMap;

    @Mock
    private PgCacheSerializerPair pgCacheSerializerPair;

    @Mock
    private PgCacheSerializer<?> keySerializer;

    @Mock
    private PgCacheSerializer<?> valueSerializer;

    @Mock
    private PgCacheSerializer<?> defaultKeySerializer;

    @Mock
    private PgCacheSerializer<?> defaultValueSerializer;

    @BeforeEach
    void init() {
        serializerPairMap = new HashMap<>();
        configuration = new PgCacheSerializerConfiguration(serializerPairMap);
    }

    @AfterEach
    void verifyNoUnexpectedInteractions() {
        Mockito.verifyNoMoreInteractions(
                keySerializer, valueSerializer, defaultKeySerializer, defaultValueSerializer
        );
    }

    @Test
    void should_return_named_serializer_pair() {
        // GIVEN
        serializerPairMap.put(CACHE_NAME, pgCacheSerializerPair);

        // WHEN
        var actual = configuration.getSerializerPair(CACHE_NAME);

        // THEN
        assertThat(actual).isEqualTo(pgCacheSerializerPair);
    }

    @Test
    void should_return_default_serializer_pair() {
        // GIVEN
        serializerPairMap.put(CACHE_NAME, null); //explicitly to show, that it is not taken from here, if it is null
        serializerPairMap.put(null, pgCacheSerializerPair);

        // WHEN
        var actual = configuration.getSerializerPair(CACHE_NAME);

        // THEN
        assertThat(actual).isEqualTo(pgCacheSerializerPair);
    }

    @Test
    void should_throw_exception_because_no_pair() {
        // GIVEN
        var e = new PgCacheSerializerConfigurationException(CACHE_NAME);
        serializerPairMap.put("otherCacheName", pgCacheSerializerPair);

        // WHEN + THEN
        assertThatThrownBy(() -> configuration.getSerializerPair(CACHE_NAME)).isInstanceOf(PgCacheSerializerConfigurationException.class)
                                                                             .hasMessage(e.getMessage());
    }

    @Test
    void should_build_correctly() {
        // GIVEN
        var expectedPair = new PgCacheSerializerPair(keySerializer, valueSerializer);
        var expectedDefaultPair = new PgCacheSerializerPair(defaultKeySerializer, defaultValueSerializer);

        // WHEN
        var actual = PgCacheSerializerConfiguration.builder()
                                                   .serializerConfiguration(CACHE_NAME, keySerializer, valueSerializer)
                                                   .serializerConfiguration(null, defaultKeySerializer, defaultValueSerializer)
                                                   .build();

        // THEN
        assertThat(actual).isNotNull();
        assertThat(actual.getSerializerPair(CACHE_NAME)).isEqualTo(expectedPair);
        assertThat(actual.getSerializerPair(null)).isEqualTo(expectedDefaultPair);
    }
}