package io.github.mfaltan.pgcache.core;

import org.springframework.lang.Nullable;

public interface ValueSerializer {
    byte[] serialize(Object value);
    <T> T deserialize(byte[] bytes, @Nullable Class<T> type);
}
