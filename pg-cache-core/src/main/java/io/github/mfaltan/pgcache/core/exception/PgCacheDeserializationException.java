package io.github.mfaltan.pgcache.core.exception;

public class PgCacheDeserializationException extends PgCacheException {

    public PgCacheDeserializationException(Exception cause) {
        super("Jackson deserialization failed", cause);
    }
}
