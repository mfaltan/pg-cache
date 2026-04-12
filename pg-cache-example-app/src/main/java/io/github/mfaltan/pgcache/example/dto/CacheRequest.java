package io.github.mfaltan.pgcache.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CacheRequest(
        @NotNull Integer age,
        @NotBlank String name
) {}