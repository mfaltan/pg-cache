package io.github.mfaltan.pgcache.core;

import java.util.concurrent.Executor;

public interface ExecutorHolder {

    Executor getWriteExecutor();

    Executor getClearExecutor();
}
