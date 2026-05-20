package io.github.mfaltan.pgcache.core.interceptor;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.core.domain.KeyEntry;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
@Builder
public class PgCacheOperationContextInner {

    private final Function<Object, Object> keyFunction;
    private final Supplier<Object> keySupplier;
    private final Supplier<Method> methodSupplier;
    private final boolean typeNeeded;

    @Nullable
    public Object generateKey(@Nullable Object result) {
        var rawKey = keyFunction.apply(result);
        if (!typeNeeded) {
            return rawKey;
        }
        log.debug(Constants.MARKER, "Generating key in pgCacheInterceptor for [{}]", result);
        log.debug(Constants.MARKER, "Raw key [{}]", rawKey);
        var ret = rawKey != null ? wrapKey(rawKey) : null;
        log.debug(Constants.MARKER, "Transformed generated key to [{}]", ret);
        return ret;
    }

    @Nullable
    public Object getGeneratedKey() {
        var rawKey = keySupplier.get();
        if (!typeNeeded) {
            return rawKey;
        }
        log.debug(Constants.MARKER, "Got generated key [{}]", rawKey);
        var ret = rawKey != null ? wrapKey(rawKey) : null;
        log.debug(Constants.MARKER, "Transformed received key to [{}]", ret);
        return ret;
    }

    private KeyEntry wrapKey(Object rawKey) {
        var type = methodSupplier.get().getGenericReturnType();
        return KeyEntry.builder()
                       .type(type)
                       .rawKey(rawKey)
                       .build();
    }
}
