package io.github.mfaltan.pgcache.core.cache;

import com.google.common.hash.Hashing;
import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.store.CacheStore;
import io.github.mfaltan.pgcache.core.serializer.CacheValueSerializer;
import io.github.mfaltan.pgcache.core.domain.CacheEntry;
import io.github.mfaltan.pgcache.core.domain.KeyEntry;
import io.github.mfaltan.pgcache.core.exception.PgCacheCallerException;
import io.github.mfaltan.pgcache.resilience.CacheResilience;
import jakarta.annotation.Nonnull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.Callable;

@Builder
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = "cacheStore")
@Slf4j
public class PgCache implements EvictableCache, TypedCache {
    private final String name;
    private final CacheValueSerializer serializer;
    private final CacheStore cacheStore;
    private final CacheResilience cacheResilience;
    private final CacheExecutorHolder cacheExecutorHolder;

    @Override
    @Nonnull
    public String getName() {
        return name;
    }

    @Override
    @Nonnull
    public Object getNativeCache() {
        return cacheStore;
    }

    @Override
    public ValueWrapper get(@Nonnull Object key) {
        var keyEntry = keyToKeyEntry(key);
        return cacheResilience.execute(() -> getInternal(keyEntry), () -> null);
    }

    @Override
    public <T> T get(@Nonnull Object key, Class<T> type) {
        var keyEntry = keyToKeyEntry(key);
        return cacheResilience.execute(() -> getInternal(keyEntry, type), () -> null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@Nonnull Object key, @Nonnull Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);

        if (wrapper != null) {
            return (T) wrapper.get();
        }
        T value;
        try {
            log.debug(Constants.MARKER, "About to call loader for key [{}] and cache [{}]", key, name);
            value = valueLoader.call();
        } catch (Exception e) {
            throw new PgCacheCallerException(e);
        }
        put(key, value);
        return value;
    }

    @Override
    public void put(@Nonnull Object key, Object value) {
        var keyEntry = keyToKeyEntry(key);
        var executor = cacheExecutorHolder.getWriteExecutor();
        log.debug(Constants.MARKER, "About to put value with key [{}] to cache [{}]", key, name);
        executor.execute(() -> cacheResilience.execute(() -> putInternal(keyEntry, value), () -> {
        }));

    }

    @Override
    public void evict(@Nonnull Object key) {
        var keyEntry = keyToKeyEntry(key);
        var executor = cacheExecutorHolder.getWriteExecutor();
        log.debug(Constants.MARKER, "About to evict value with key [{}] from cache [{}]", key, name);
        executor.execute(() -> cacheResilience.execute(() -> evictInternal(keyEntry), () -> {
        }));
    }

    @Override
    public void clear() {
        var executor = cacheExecutorHolder.getClearExecutor();
        log.debug(Constants.MARKER, "About to clear cache [{}]", name);
        executor.execute(() -> cacheResilience.execute(cacheStore::clear, () -> {
        }));
    }

    @Override
    public void evictExpired(int limit) {
        var executor = cacheExecutorHolder.getWriteExecutor();
        log.debug(Constants.MARKER, "About to evict expired from cache [{}]", name);
        executor.execute(() -> cacheResilience.execute(() -> cacheStore.evictExpired(limit), () -> {
        }));
    }

    private ValueWrapper getInternal(KeyEntry keyEntry) {
        CacheEntry data = getCacheEntry(keyEntry);
        if (data == null) return null;

        Object value = serializer.deserialize(data.value(), keyEntry.type());
        return () -> value;
    }

    private <T> T getInternal(KeyEntry keyEntry, Class<T> type) {
        CacheEntry data = getCacheEntry(keyEntry);
        if (data == null) {
            return null;
        } else {
            return serializer.deserialize(data.value(), type);
        }
    }

    private void putInternal(KeyEntry keyEntry, Object value) {
        byte[] normalizedKey = normalizeKey(keyEntry);
        Long longKey = generateKey(normalizedKey);

        byte[] serializedValue = serializer.serialize(value);

        var entry = CacheEntry.builder()
                              .normalizedKey(normalizedKey)
                              .value(serializedValue)
                              .build();

        cacheStore.put(longKey, entry);
    }

    private void evictInternal(KeyEntry keyEntry) {
        byte[] normalizedKey = normalizeKey(keyEntry);
        Long longKey = generateKey(normalizedKey);
        cacheStore.remove(longKey);
    }

    private Long generateKey(byte[] normalizedKey) {
        return Hashing.murmur3_128()
                      .hashBytes(normalizedKey)
                      .asLong();
    }

    private byte[] normalizeKey(KeyEntry keyEntry) {
        return serializer.serialize(keyEntry.rawKey());
    }

    private KeyEntry keyToKeyEntry(Object key) {
        if (key instanceof KeyEntry keyEntry) {
            return keyEntry;
        } else {
            throw new IllegalArgumentException("Provided key is not KeyEntry");
        }
    }

    private CacheEntry getCacheEntry(KeyEntry key) {
        byte[] normalizedKey = normalizeKey(key);
        Long longKey = generateKey(normalizedKey);

        CacheEntry data = cacheStore.get(longKey);
        if (data == null || !Arrays.equals(data.normalizedKey(), normalizedKey)) return null;
        return data;
    }
}
