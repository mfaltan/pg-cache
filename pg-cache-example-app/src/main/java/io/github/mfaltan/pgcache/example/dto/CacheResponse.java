package io.github.mfaltan.pgcache.example.dto;

import java.util.List;
import java.util.UUID;

public record CacheResponse(
        List<UUID> uuids
) {}