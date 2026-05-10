package io.github.mfaltan.pgcache.perftest.dto;

import lombok.Builder;

@Builder
public record CacheRequest(
        Integer age,
        Integer age2,
        String name
) {

}