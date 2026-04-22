package io.github.mfaltan.pgcache.core.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.core.exception.PgCacheDeserializationException;
import io.github.mfaltan.pgcache.core.exception.PgCacheSerializationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;

@RequiredArgsConstructor
@Slf4j
public class PgCacheSerializer implements CacheValueSerializer {
    private final ObjectMapper mapper;

    @Override
    public byte[] serialize(Object value) {
        try {
            log.debug(Constants.MARKER, "Writing value [{}] to bytes", value);
            return mapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new PgCacheSerializationException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) {
        try {
            log.debug(Constants.MARKER, "Deserializing bytes to class [{}]", type);
            var ret = type != null ? mapper.readValue(bytes, type) : null;
            log.trace(Constants.MARKER, "Deserialized to [{}]", ret);
            return ret;
        } catch (IOException e) {
            throw new PgCacheDeserializationException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Type type) {
        log.debug(Constants.MARKER, "Deserializing bytes to type [{}]", type);
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        log.debug(Constants.MARKER, "Identified java type [{}]", javaType);

        try {
            T ret = mapper.readValue(bytes, javaType);
            log.debug(Constants.MARKER, "Deserialized to [{}]", ret);
            return ret;
        } catch (IOException e) {
            throw new PgCacheDeserializationException(e);
        }
    }
}
