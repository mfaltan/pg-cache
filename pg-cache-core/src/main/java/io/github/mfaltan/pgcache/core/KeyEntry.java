package io.github.mfaltan.pgcache.core;

import lombok.Builder;

import java.lang.reflect.Type;

@Builder
public record KeyEntry(Type type, Object rawKey) {}
