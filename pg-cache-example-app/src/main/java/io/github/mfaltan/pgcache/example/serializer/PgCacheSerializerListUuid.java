package io.github.mfaltan.pgcache.example.serializer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class PgCacheSerializerListUuid implements PgCacheSerializer<List<UUID>> {
    private final ObjectMapper mapper;

    @Override
    @SneakyThrows //just test purposes
    public byte[] serializeValue(List<UUID> value) {
        log.info("Called custom serializer.serialize");
        return mapper.writeValueAsBytes(value);
    }

    @Override
    @SneakyThrows //just test purposes
    public List<UUID> deserialize(byte[] bytes) {
        log.info("Called custom serializer.deserialize");
        return mapper.readValue(bytes,  new TypeReference<>() {
        });
    }
}
