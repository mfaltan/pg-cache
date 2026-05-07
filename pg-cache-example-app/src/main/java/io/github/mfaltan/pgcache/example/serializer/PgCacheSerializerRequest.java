package io.github.mfaltan.pgcache.example.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializer;
import io.github.mfaltan.pgcache.example.dto.CacheRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PgCacheSerializerRequest implements PgCacheSerializer<CacheRequest> {
    private final ObjectMapper mapper;

    @Override
    @SneakyThrows //just test purposes
    public byte[] serializeValue(CacheRequest value) {
        log.info("Called custom serializer.serialize");
        return mapper.writeValueAsBytes(value);
    }

    @Override
    @SneakyThrows //just test purposes
    public CacheRequest deserialize(byte[] bytes) {
        log.info("Called custom serializer.deserialize");
        return mapper.readValue(bytes, CacheRequest.class);
    }
}
