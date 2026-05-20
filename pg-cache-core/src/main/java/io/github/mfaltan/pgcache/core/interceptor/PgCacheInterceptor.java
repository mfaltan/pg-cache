package io.github.mfaltan.pgcache.core.interceptor;

import io.github.mfaltan.pgcache.core.cache.PgCache;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperation;

import java.lang.reflect.Method;

@Slf4j
public class PgCacheInterceptor extends CacheInterceptor {

    @Override
    @Nonnull
    protected CacheOperationContext getOperationContext(@Nonnull CacheOperation operation,
                                                        @Nonnull Method method,
                                                        @Nonnull Object[] args,
                                                        @Nonnull Object target,
                                                        @Nonnull Class<?> targetClass) {

        CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
        return new PgCacheOperationContext(metadata, args, target);
    }

    protected class PgCacheOperationContext extends CacheOperationContext {

        private final PgCacheOperationContextInner inner;

        public PgCacheOperationContext(CacheOperationMetadata metadata, @Nullable Object[] args, Object target) {
            super(metadata, args, target);
            var typeNeeded = this.getCaches()
                                 .stream()
                                 .anyMatch(cache -> cache instanceof PgCache);
            inner = PgCacheOperationContextInner.builder()
                                                .methodSupplier(this::getMethod)
                                                .keyFunction(super::generateKey)
                                                .keySupplier(super::getGeneratedKey)
                                                .typeNeeded(typeNeeded)
                                                .build();
        }

        @Override
        protected @Nullable Object generateKey(@Nullable Object result) {
            return inner.generateKey(result);
        }

        @Override
        protected @Nullable Object getGeneratedKey() {
            return inner.getGeneratedKey();
        }
    }
}
