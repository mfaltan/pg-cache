package io.github.mfaltan.pgcache.core.serializer;

public record PgCacheSerializerPair(PgCacheSerializer<?> keySerializer, PgCacheSerializer<?> valueSerializer) {
}
