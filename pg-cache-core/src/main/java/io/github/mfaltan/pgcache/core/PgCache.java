package io.github.mfaltan.pgcache.core;

import com.google.common.hash.Hashing;
import io.github.mfaltan.pgcache.core.exception.PgCacheCallerException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

@Builder
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = "store")
public class PgCache implements Cache {
    private final String name;
    private final ValueSerializer serializer;
    private final ConcurrentHashMap<Long, CacheEntry> store = new ConcurrentHashMap<>();

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
        CacheEntry data = getCacheEntry(key);
        if (data == null) return null;

        Object value = serializer.deserialize(data.value(), Object.class);
        return () -> value;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        CacheEntry data = getCacheEntry(key);
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
        byte[] normalizedKey = normalizeKey(key);
        Long longKey = generateKey(normalizedKey);

        byte[] serializedValue = serializer.serialize(value);

        var entry = CacheEntry.builder()
                              .normalizedKey(normalizedKey)
                              .value(serializedValue).build();

        store.put(longKey, entry);
    }

    @Override
    public void evict(Object key) {
        byte[] normalizedKey = normalizeKey(key);
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

    private byte[] normalizeKey(Object key) {
        return serializer.serialize(key);
    }

    private CacheEntry getCacheEntry(Object key) {
        byte[] normalizedKey = normalizeKey(key);
        Long longKey = generateKey(normalizedKey);

        CacheEntry data = store.get(longKey);
        if (data == null || !Arrays.equals(data.normalizedKey(), normalizedKey)) return null;
        return data;
    }
}
