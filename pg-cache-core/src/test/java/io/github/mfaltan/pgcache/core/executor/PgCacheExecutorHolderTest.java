package io.github.mfaltan.pgcache.core.executor;

import io.github.mfaltan.pgcache.common.PgCacheProperties.AsyncProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgCacheExecutorHolderTest {

    private static final int WRITE_THREADS = 1;
    private static final int CLEAR_THREADS = 2;
    private static final int WRITE_QUEUE = 3;
    private static final int CLEAR_QUEUE = 4;

    @InjectMocks
    private PgCacheExecutorHolder holder;

    @Mock
    private AsyncProperties asyncProperties;

    @Mock
    private TaskDecorator taskDecorator;

    @Mock
    private Runnable runnable1, runnable2;

    @Mock
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setup() {
        holder = new PgCacheExecutorHolder(asyncProperties, taskDecorator);
    }

    @Test
    void should_not_initialize_use_runnable_and_do_not_destroy() {
        //GIVEN
        when(asyncProperties.isEnabled()).thenReturn(false);

        //WHEN + THEN
        holder.initialize();
        var writeExecutor = holder.getWriteExecutor();
        var clearExecutor= holder.getClearExecutor();
        writeExecutor.execute(runnable1);
        clearExecutor.execute(runnable2);
        assertThatCode(() -> holder.destroy()).doesNotThrowAnyException();

        verifyNoMoreInteractions(asyncProperties);
        verify(runnable1).run();
        verify(runnable2).run();
    }

    @Test
    void should_properly_initialize_and_destroy() {
        //GIVEN
        PgCacheExecutorHolder spiedHolder = Mockito.spy(holder);
        when(asyncProperties.isEnabled()).thenReturn(true);
        when(asyncProperties.getWriteThreads()).thenReturn(WRITE_THREADS);
        when(asyncProperties.getClearThreads()).thenReturn(CLEAR_THREADS);
        when(asyncProperties.getWriteQueue()).thenReturn(WRITE_QUEUE);
        when(asyncProperties.getClearQueue()).thenReturn(CLEAR_QUEUE);

        doNothing().when(spiedHolder).initializeExecutor(any(ThreadPoolTaskExecutor.class));
        doNothing().when(spiedHolder).shutdownExecutor(any(ThreadPoolTaskExecutor.class));

        //WHEN
        spiedHolder.initialize();
        var writeExecutor = (ThreadPoolTaskExecutor) spiedHolder.getWriteExecutor();
        var clearExecutor = (ThreadPoolTaskExecutor) spiedHolder.getClearExecutor();
        spiedHolder.destroy();

        //THEN
        verifyNoMoreInteractions(asyncProperties);

        assertThat(writeExecutor.getCorePoolSize()).isEqualTo(WRITE_THREADS);
        assertThat(writeExecutor.getMaxPoolSize()).isEqualTo(WRITE_THREADS);
        assertThat(writeExecutor.getQueueCapacity()).isEqualTo(WRITE_QUEUE);
        verify(spiedHolder).setTaskDecorator(writeExecutor);
        verify(spiedHolder).initializeExecutor(writeExecutor);
        verify(spiedHolder).shutdownExecutor(writeExecutor);

        assertThat(clearExecutor.getCorePoolSize()).isEqualTo(CLEAR_THREADS);
        assertThat(clearExecutor.getMaxPoolSize()).isEqualTo(CLEAR_THREADS);
        assertThat(clearExecutor.getQueueCapacity()).isEqualTo(CLEAR_QUEUE);
        verify(spiedHolder).setTaskDecorator(clearExecutor);
        verify(spiedHolder).initializeExecutor(clearExecutor);
        verify(spiedHolder).shutdownExecutor(clearExecutor);
    }

    @Test
    void should_initialize_executor(){
        //WHEN
        holder.initializeExecutor(executor);

        //THEN
        verify(executor).initialize();
    }

    @Test
    void should_set_decorator_to_executor(){
        //WHEN
        holder.setTaskDecorator(executor);

        //THEN
        verify(executor).setTaskDecorator(taskDecorator);
    }

    @Test
    void should_shutdown_executor(){
        //WHEN
        holder.shutdownExecutor(executor);

        //THEN
        verify(executor).shutdown();
    }
}