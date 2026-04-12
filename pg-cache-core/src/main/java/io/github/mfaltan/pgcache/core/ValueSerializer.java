package io.github.mfaltan.pgcache.core;

import java.lang.reflect.Type;

public interface ValueSerializer {
    byte[] serialize(Object value);
    <T> T deserialize(byte[] bytes, Class<T> type);

    <T> T deserialize(byte[] bytes, Type type);
}
