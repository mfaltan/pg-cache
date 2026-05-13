package io.github.mfaltan.pgcache.core.serializer;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgCacheSerializerStringTest {

    private static final byte[] BYTES = new byte[]{1, 2, 3};
    private static final String VALUE = "test";

    @InjectMocks
    private PgCacheSerializerString serializer;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private JsonProcessingException jsonProcessingException;

    @Mock
    private IOException ioException;

    @Test
    void should_return_bytes_when_serialize() throws Exception {
        // GIVEN
        when(mapper.writeValueAsBytes(VALUE)).thenReturn(BYTES);

        // WHEN
        var actual = serializer.serializeValue(VALUE);

        // THEN
        assertThat(actual).isEqualTo(BYTES);
    }

    @Test
    void should_wrap_exception_when_serialize() throws Exception {
        // GIVEN
        when(mapper.writeValueAsBytes(VALUE)).thenThrow(jsonProcessingException);

        // WHEN + THEN
        assertThatThrownBy(() -> serializer.serializeValue(VALUE)).isInstanceOf(PgCacheSerializationException.class)
                                                                  .hasCause(jsonProcessingException);
    }

    @Test
    void should_return_string_when_deserialize() throws Exception {
        // GIVEN
        when(mapper.readValue(BYTES, String.class)).thenReturn(VALUE);

        // WHEN
        var actual = serializer.deserialize(BYTES);

        // THEN
        assertThat(actual).isEqualTo(VALUE);
    }

    @Test
    void should_wrap_exception_when_deserialize() throws Exception {
        // GIVEN
        when(mapper.readValue(BYTES, String.class)).thenThrow(ioException);

        // WHEN + THEN
        assertThatThrownBy(() -> serializer.deserialize(BYTES)).isInstanceOf(PgCacheDeserializationException.class)
                                                               .hasCause(ioException);
    }
}