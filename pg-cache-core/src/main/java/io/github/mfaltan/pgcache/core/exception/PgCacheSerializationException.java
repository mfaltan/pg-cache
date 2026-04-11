package io.github.mfaltan.pgcache.core.exception;

public class PgCacheSerializationException extends PgCacheException {

    public PgCacheSerializationException(Exception cause) {
        super("Jackson serialization failed", cause);
    }
}
