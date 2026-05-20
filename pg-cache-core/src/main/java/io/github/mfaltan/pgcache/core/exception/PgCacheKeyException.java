package io.github.mfaltan.pgcache.core.exception;

import java.text.MessageFormat;

public class PgCacheKeyException extends PgCacheException {

    public PgCacheKeyException(Object key) {
        super(MessageFormat.format("Provided key [{0}] is invalid. It is neither key-entry, nor there is dedicated serializer that can work without type", key));
    }
}
