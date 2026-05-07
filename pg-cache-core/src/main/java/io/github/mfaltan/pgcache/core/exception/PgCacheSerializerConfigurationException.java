package io.github.mfaltan.pgcache.core.exception;

import java.text.MessageFormat;

public class PgCacheSerializerConfigurationException extends PgCacheException {

    public PgCacheSerializerConfigurationException(String name) {
        super(MessageFormat.format("Missing serializer config for cache [{0}]", name));
    }
}
