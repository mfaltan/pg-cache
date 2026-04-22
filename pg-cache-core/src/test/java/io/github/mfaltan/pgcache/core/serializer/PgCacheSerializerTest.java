package io.github.mfaltan.pgcache.core.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.github.mfaltan.pgcache.core.exception.PgCacheDeserializationException;
import io.github.mfaltan.pgcache.core.exception.PgCacheSerializationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgCacheSerializerTest {

    private static final String VALUE = "test-value";
    private static final byte[] BYTES = new byte[]{1, 2, 3};
    private static final Class<String> CLAZZ = String.class;

    @InjectMocks
    private PgCacheSerializer serializer;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private TypeFactory typeFactory;

    @Mock
    private JavaType javaType;

    @Mock
    private Type type;

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
        when(mapper.readValue(BYTES, CLAZZ)).thenReturn(VALUE);

        // WHEN
        var result = serializer.deserialize(BYTES, CLAZZ);

        // THEN
        assertThat(result).isEqualTo(VALUE);
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void should_deserialize_typed_value() throws Exception {
        // GIVEN
        when(mapper.getTypeFactory()).thenReturn(typeFactory);
        when(typeFactory.constructType(type)).thenReturn(javaType);
        when(mapper.readValue(BYTES, javaType)).thenReturn(VALUE);

        // WHEN
        var result = serializer.deserialize(BYTES, type);

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
        when(mapper.readValue(BYTES, CLAZZ)).thenThrow(IOException.class);

        // WHEN / THEN
        assertThatThrownBy(() -> serializer.deserialize(BYTES, CLAZZ))
                .isInstanceOf(PgCacheDeserializationException.class);

        verifyNoMoreInteractions(mapper);
    }

    @Test
    void should_throw_exception_when_typed_deserialization_fails() throws Exception {
        // GIVEN
        when(mapper.getTypeFactory()).thenReturn(typeFactory);
        when(typeFactory.constructType(type)).thenReturn(javaType);
        when(mapper.readValue(BYTES, javaType)).thenThrow(IOException.class);

        // WHEN / THEN
        assertThatThrownBy(() -> serializer.deserialize(BYTES, type))
                .isInstanceOf(PgCacheDeserializationException.class);

        verifyNoMoreInteractions(mapper);
    }
}