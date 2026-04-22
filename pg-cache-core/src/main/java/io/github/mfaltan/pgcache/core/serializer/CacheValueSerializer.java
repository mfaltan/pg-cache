package io.github.mfaltan.pgcache.core.serializer;

import java.lang.reflect.Type;

public interface CacheValueSerializer {
    byte[] serialize(Object value);
    <T> T deserialize(byte[] bytes, Class<T> type);

    <T> T deserialize(byte[] bytes, Type type);
}
