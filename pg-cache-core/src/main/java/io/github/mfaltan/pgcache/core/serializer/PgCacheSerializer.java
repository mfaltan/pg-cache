package io.github.mfaltan.pgcache.core.serializer;

public interface PgCacheSerializer<V> {

    byte[] serializeValue(V value);

    V deserialize(byte[] bytes);
}
