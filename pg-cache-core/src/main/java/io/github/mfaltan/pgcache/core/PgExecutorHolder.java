package io.github.mfaltan.pgcache.core;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.common.PgCacheProperties.AsyncProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@RequiredArgsConstructor
public class PgExecutorHolder implements ExecutorHolder {

    private final AsyncProperties asyncProperties;
    private final TaskDecorator taskDecorator;

    private static final Executor SYNC_EXECUTOR = Runnable::run;

    private ThreadPoolTaskExecutor writeExecutor;
    private ThreadPoolTaskExecutor clearExecutor;

    @PostConstruct
    public void initialize() {
        if (asyncProperties.isEnabled()) {
            log.info(Constants.MARKER, "Initializing write executor service");
            writeExecutor = createExecutor(asyncProperties.getWriteThreads(), asyncProperties.getWriteQueue());
            log.info(Constants.MARKER, "Initializing clear executor service");
            clearExecutor = createExecutor(asyncProperties.getClearThreads(), asyncProperties.getClearQueue());
        }
    }

    @PreDestroy
    public void destroy() {
        if (writeExecutor != null) {
            log.info(Constants.MARKER, "Shutting down write executor service");
            writeExecutor.shutdown();
        }

        if (clearExecutor != null) {
            log.info(Constants.MARKER, "Shutting down clear executor service");
            clearExecutor.shutdown();
        }
    }

    @Override
    public Executor getWriteExecutor() {
        if (writeExecutor != null) {
            log.debug(Constants.MARKER, "Using asynchronous approach to write to the cache");
            return writeExecutor;
        } else {
            log.debug(Constants.MARKER, "Using synchronous approach to write to the cache");
            return SYNC_EXECUTOR;
        }
    }

    @Override
    public Executor getClearExecutor() {
        if (clearExecutor != null) {
            log.debug(Constants.MARKER, "Using asynchronous approach to clear the cache");
            return clearExecutor;
        } else {
            log.debug(Constants.MARKER, "Using synchronous approach to clear the cache");
            return SYNC_EXECUTOR;
        }
    }

    private ThreadPoolTaskExecutor createExecutor(int threads, int queue) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(threads);
        ex.setMaxPoolSize(threads);
        ex.setQueueCapacity(queue);
        ex.setTaskDecorator(taskDecorator);
        ex.initialize();
        return ex;
    }
}
