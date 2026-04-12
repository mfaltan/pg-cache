package io.github.mfaltan.pgcache.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.core.exception.PgCacheDeserializationException;
import io.github.mfaltan.pgcache.core.exception.PgCacheSerializationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JacksonSerializerTest {

    private static final String VALUE = "test-value";
    private static final byte[] BYTES = new byte[]{1, 2, 3};
    private static final Class<String> TYPE = String.class;

    @InjectMocks
    private JacksonSerializer serializer;

    @Mock
    private ObjectMapper mapper;

    @Test
    void should_serialize_value() throws Exception {
        // GIVEN
        when(mapper.writeValueAsBytes(VALUE)).thenReturn(BYTES);

        // WHEN
        var result = serializer.serialize(VALUE);

        // THEN
        assertThat(result).isEqualTo(BYTES);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void should_throw_exception_when_serialization_fails() throws Exception {
        // GIVEN
        when(mapper.writeValueAsBytes(VALUE)).thenThrow(JsonProcessingException.class);

        // WHEN / THEN
        assertThatThrownBy(() -> serializer.serialize(VALUE))
                .isInstanceOf(PgCacheSerializationException.class);

        verifyNoMoreInteractions(mapper);
    }

    @Test
    void should_deserialize_value() throws Exception {
        // GIVEN
        when(mapper.readValue(BYTES, TYPE)).thenReturn(VALUE);

        // WHEN
        var result = serializer.deserialize(BYTES, TYPE);

        // THEN
        assertThat(result).isEqualTo(VALUE);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void should_return_null_when_type_is_null() {
        // WHEN
        var result = serializer.deserialize(BYTES, null);

        // THEN
        assertThat(result).isNull();
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void should_throw_exception_when_deserialization_fails() throws Exception {
        // GIVEN
        when(mapper.readValue(BYTES, TYPE)).thenThrow(IOException.class);

        // WHEN / THEN
        assertThatThrownBy(() -> serializer.deserialize(BYTES, TYPE))
                .isInstanceOf(PgCacheDeserializationException.class);

        verifyNoMoreInteractions(mapper);
    }
}