package io.github.mfaltan.pgcache.core;

import jakarta.annotation.Nonnull;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

public class PgCacheTaskDecorator implements TaskDecorator {

    @Override
    @Nonnull public Runnable decorate(@Nonnull Runnable runnable) {

        Map<String, String> context = MDC.getCopyOfContextMap();

        return () -> {
            try {
                if (context != null) {
                    MDC.setContextMap(context);
                } else {
                    MDC.clear();
                }

                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}