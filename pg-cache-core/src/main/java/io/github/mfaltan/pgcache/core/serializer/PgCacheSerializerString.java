package io.github.mfaltan.pgcache.core.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.core.exception.PgCacheDeserializationException;
import io.github.mfaltan.pgcache.core.exception.PgCacheSerializationException;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class PgCacheSerializerString implements PgCacheSerializer<String> {

    private final ObjectMapper mapper;

    @Override
    public byte[] serializeValue(String value) {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new PgCacheSerializationException(e);
        }
    }

    @Override
    public String deserialize(byte[] bytes) {
        try {
            return mapper.readValue(bytes, String.class);
        } catch (IOException e) {
            throw new PgCacheDeserializationException(e);
        }
    }
}
