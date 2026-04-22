package io.github.mfaltan.pgcache.core;

import io.github.mfaltan.pgcache.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperation;

import java.lang.reflect.Method;

@Slf4j
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
            log.debug(Constants.MARKER, "Generating key in pgCacheInterceptor for [{}]", result);
            var rawKey = super.generateKey(result);
            log.debug(Constants.MARKER, "Raw key [{}]", rawKey);
            var ret = transformKey(rawKey);
            log.debug(Constants.MARKER, "Transformed generated key to [{}]", ret);
            return ret;
        }

        @Override
        protected @Nullable Object getGeneratedKey() {
            var rawKey = super.getGeneratedKey();
            log.debug(Constants.MARKER, "Got generated key [{}]", rawKey);
            var ret = transformKey(rawKey);
            log.debug(Constants.MARKER, "Transformed received key to [{}]", ret);
            return ret;
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
