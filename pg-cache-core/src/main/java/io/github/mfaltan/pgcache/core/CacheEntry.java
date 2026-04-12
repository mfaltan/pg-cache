package io.github.mfaltan.pgcache.core;

import lombok.Builder;

import java.lang.reflect.Type;

@Builder
public record CacheEntry (

    byte[] normalizedKey,  // original key (serialized if needed)
    byte[] value,// serialized value
    Type type
) {}
