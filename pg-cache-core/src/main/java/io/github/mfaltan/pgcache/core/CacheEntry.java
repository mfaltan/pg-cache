package io.github.mfaltan.pgcache.core;

import lombok.Builder;

@Builder
public record CacheEntry (

    byte[] normalizedKey,  // original key (serialized if needed)
    byte[] value// serialized value

) {}
