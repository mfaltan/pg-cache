package io.github.mfaltan.pgcache.core;

import com.google.common.hash.Hashing;
import io.github.mfaltan.pgcache.core.exception.PgCacheCallerException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;

import java.util.Arrays;
import java.util.concurrent.Callable;

@Builder
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = "store")
public class PgCache implements Cache, TypedCache {
    private final String name;
    private final ValueSerializer serializer;
    private final Store store;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return store;
    }

    @Override
    public ValueWrapper get(Object key) {
        var keyEntry = keyToKeyEntry(key);
        CacheEntry data = getCacheEntry(keyEntry);
        if (data == null) return null;

        Object value = serializer.deserialize(data.value(), keyEntry.type());
        return () -> value;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        var keyEntry = keyToKeyEntry(key);
        CacheEntry data = getCacheEntry(keyEntry);
        if (data == null) {
            return null;
        } else {
            return serializer.deserialize(data.value(), type);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);

        if (wrapper != null) {
            return (T) wrapper.get();
        }
        T value;
        try {
            value = valueLoader.call();
        } catch (Exception e) {
            throw new PgCacheCallerException(e);
        }
        put(key, value);
        return value;
    }

    @Override
    public void put(Object key, Object value) {
        var keyEntry = keyToKeyEntry(key);
        byte[] normalizedKey = normalizeKey(keyEntry);
        Long longKey = generateKey(normalizedKey);

        byte[] serializedValue = serializer.serialize(value);

        var entry = CacheEntry.builder()
                              .normalizedKey(normalizedKey)
                              .value(serializedValue)
                              .build();

        store.put(longKey, entry);
    }

    @Override
    public void evict(Object key) {
        var keyEntry = keyToKeyEntry(key);
        byte[] normalizedKey = normalizeKey(keyEntry);
        Long longKey = generateKey(normalizedKey);
        store.remove(longKey);
    }

    @Override
    public void clear() {
        store.clear();
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
        if (key instanceof KeyEntry) {
            return (KeyEntry) key;
        } else {
            throw new IllegalArgumentException("Provided key is not KeyEntry");
        }
    }

    private CacheEntry getCacheEntry(KeyEntry key) {
        byte[] normalizedKey = normalizeKey(key);
        Long longKey = generateKey(normalizedKey);

        CacheEntry data = store.get(longKey);
        if (data == null || !Arrays.equals(data.normalizedKey(), normalizedKey)) return null;
        return data;
    }
}
