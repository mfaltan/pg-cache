package io.github.mfaltan.pgcache.core.exception;

public class PgCacheCallerException extends PgCacheException {

    public PgCacheCallerException(Exception cause) {
        super("Call for new value failed", cause);
    }
}
