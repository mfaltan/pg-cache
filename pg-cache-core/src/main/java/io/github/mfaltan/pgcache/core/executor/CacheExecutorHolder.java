package io.github.mfaltan.pgcache.core.executor;

import java.util.concurrent.Executor;

public interface CacheExecutorHolder {

    Executor getWriteExecutor();

    Executor getClearExecutor();
}
