package io.github.mfaltan.pgcache.example.dto;

import lombok.Builder;

@Builder
public record CacheRequest(
        Integer age,
        Integer age2,
        String name
) {

}