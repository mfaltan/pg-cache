package io.github.mfaltan.pgcache.core.serializer;

import jakarta.annotation.Nullable;

import java.lang.reflect.Type;
import java.util.Collection;

public interface CacheValueSerializer {

    @Nullable Collection<String> getCacheNames();

    byte[] serialize(Object value);

    <T> T deserialize(byte[] bytes, Class<T> type);

    <T> T deserialize(byte[] bytes, Type type);
}
