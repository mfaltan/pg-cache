package io.github.mfaltan.pgcache.core.util;

import java.time.LocalDateTime;

public interface CurrentDateTimeProvider {

    LocalDateTime now();
}