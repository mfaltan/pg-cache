package io.github.mfaltan.pgcache.core.exception;

public class PgCacheException extends RuntimeException {

    public PgCacheException(String message, Exception cause) {
        super(message, cause);
    }
}
