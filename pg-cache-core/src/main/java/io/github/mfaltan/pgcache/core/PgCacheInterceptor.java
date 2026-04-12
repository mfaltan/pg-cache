package io.github.mfaltan.pgcache.core;

import org.jspecify.annotations.Nullable;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperation;

import java.lang.reflect.Method;

public class PgCacheInterceptor extends CacheInterceptor {

    @Override
    protected CacheOperationContext getOperationContext(CacheOperation operation, Method method, @Nullable Object[] args, Object target, Class<?> targetClass) {

        CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
        return new PgCacheOperationContext(metadata, args, target);
    }

    protected class PgCacheOperationContext extends CacheOperationContext {

        private final boolean typeNeeded;

        public PgCacheOperationContext(CacheOperationMetadata metadata, @Nullable Object[] args, Object target) {
            super(metadata, args, target);
            typeNeeded = this.getCaches()
                             .stream()
                             .anyMatch(cache -> cache instanceof TypedCache);
        }

        @Override
        protected @Nullable Object generateKey(@Nullable Object result) {
            var rawKey = super.generateKey(result);
            return transformKey(rawKey);
        }

        @Override
        protected @Nullable Object getGeneratedKey() {
            var rawKey = super.getGeneratedKey();
            return transformKey(rawKey);
        }

        private Object transformKey(Object rawKey) {
            if (rawKey == null) {
                return null;
            }
            return typeNeeded ? wrapKey(rawKey) : rawKey;
        }

        private KeyEntry wrapKey(Object rawKey) {
            var type = getMethod().getGenericReturnType();
            return KeyEntry.builder()
                           .type(type)
                           .rawKey(rawKey)
                           .build();
        }
    }
}
