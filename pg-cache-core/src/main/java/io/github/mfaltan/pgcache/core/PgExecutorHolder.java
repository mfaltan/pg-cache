package io.github.mfaltan.pgcache.core;

import io.github.mfaltan.pgcache.common.PgCacheProperties.AsyncProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class PgExecutorHolder implements ExecutorHolder {

    private final AsyncProperties asyncProperties;

    private static final Executor SYNC_EXECUTOR = Runnable::run;

    private ExecutorService writeExecutorService;
    private ExecutorService clearExecutorService;

    @PostConstruct
    public void initialize() {
        if (asyncProperties.isEnabled()) {
            writeExecutorService = createExecutor(asyncProperties.getWriteThreads(), asyncProperties.getWriteQueue());
            clearExecutorService = createExecutor(asyncProperties.getClearThreads(), asyncProperties.getClearQueue());
        }
    }

    @PreDestroy
    public void destroy() {
        if (writeExecutorService != null) {
            writeExecutorService.shutdown();
        }

        if (clearExecutorService != null) {
            clearExecutorService.shutdown();
        }
    }

    @Override
    public Executor getWriteExecutor() {
        return writeExecutorService != null ? writeExecutorService : SYNC_EXECUTOR;
    }

    @Override
    public Executor getClearExecutor() {
        return clearExecutorService != null ? clearExecutorService : SYNC_EXECUTOR;
    }

    private ThreadPoolExecutor createExecutor(int threads, int queue) {
        return new ThreadPoolExecutor(
                threads,
                threads,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queue),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
