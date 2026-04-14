package io.github.mfaltan.pgcache.core;

import java.time.LocalDateTime;

public interface CurrentDateTimeProvider {
    LocalDateTime now();
}