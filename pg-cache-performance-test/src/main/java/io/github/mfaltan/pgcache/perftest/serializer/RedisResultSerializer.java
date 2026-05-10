package io.github.mfaltan.pgcache.perftest.serializer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class RedisResultSerializer implements RedisSerializer<List<UUID>> {

    private final ObjectMapper mapper;

    @Override
    @SneakyThrows //just test purposes
    public byte[] serialize(@Nullable List<UUID> value) throws SerializationException {
        return mapper.writeValueAsBytes(value);
    }

    @Override
    @SneakyThrows //just test purposes
    public @Nullable List<UUID> deserialize(byte @Nullable [] bytes) throws SerializationException {
        return mapper.readValue(bytes,  new TypeReference<>() {
        });
    }
}
