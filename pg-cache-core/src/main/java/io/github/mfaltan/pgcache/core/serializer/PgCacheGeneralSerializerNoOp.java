package io.github.mfaltan.pgcache.core.serializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;

@RequiredArgsConstructor
@Slf4j
public class PgCacheGeneralSerializerNoOp implements PgCacheGeneralSerializer {

    @Override
    public byte[] serialize(Object value) {
       throw new UnsupportedOperationException();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Type type) {
        throw new UnsupportedOperationException();
    }
}
