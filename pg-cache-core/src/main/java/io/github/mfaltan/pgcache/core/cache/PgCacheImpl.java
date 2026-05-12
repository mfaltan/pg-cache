package io.github.mfaltan.pgcache.core.cache;

import com.google.common.hash.Hashing;
import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.core.domain.CacheEntry;
import io.github.mfaltan.pgcache.core.domain.KeyEntry;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.PgCacheGeneralSerializer;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializer;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerPair;
import io.github.mfaltan.pgcache.core.store.PgCacheStore;
import io.github.mfaltan.pgcache.resilience.CacheResilience;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class PgCacheImpl implements PgCache {
    private final String name;
    private final PgCacheStore store;
    private final CacheExecutorHolder executorHolder;
    private final CacheResilience resilience;
    private final PgCacheGeneralSerializer generalSerializer;
    private final PgCacheSerializerPair serializerPair;
    private final PgCacheNoOp cacheNoOp;
    private final int ttlSeconds;
    private final int asyncGetTimeout;
    private final int asyncGetWithLoaderTimeout;

    public PgCacheImpl(String name,
                       PgCacheStore store,
                       CacheExecutorHolder executorHolder,
                       CacheResilience resilience,
                       PgCacheGeneralSerializer generalSerializer,
                       PgCacheSerializerPair serializerPair,
                       PgCacheProperties properties) {
        this.name = name;
        this.store = store;
        this.executorHolder = executorHolder;
        this.resilience = resilience;
        this.generalSerializer = generalSerializer;
        this.serializerPair = serializerPair;
        this.cacheNoOp = new PgCacheNoOp(name, PgCacheNoOp.Type.TEMPORARILY);

        var prop = properties.getCaches().get(name);
        this.ttlSeconds = prop != null && prop.getTtlSeconds() != null ? prop.getTtlSeconds() : properties.getDefaultTtlSeconds();
        this.asyncGetTimeout = properties.getAsyncGetTimeout();
        this.asyncGetWithLoaderTimeout = properties.getAsyncGetWithLoaderTimeout();

    }

    public PgCacheImpl(String name,
                       PgCacheStore store,
                       CacheExecutorHolder executorHolder,
                       CacheResilience resilience,
                       PgCacheGeneralSerializer generalSerializer,
                       PgCacheProperties properties) {
        this(name, store, executorHolder, resilience, generalSerializer, null, properties);
    }

    @Override
    @Nonnull
    public String getName() {
        return name;
    }

    @Override
    @Nonnull
    public Object getNativeCache() {
        return store;
    }

    @Override
    public ValueWrapper get(@Nonnull Object key) {
        var keyEntry = keyToKeyEntry(key);
        return resilience.execute(() -> getInternal(keyEntry), () -> cacheNoOp.get(key));
    }

    @Override
    public <T> T get(@Nonnull Object key, Class<T> type) {
        var keyEntry = keyToKeyEntry(key);
        return resilience.execute(() -> getInternal(keyEntry, type), () -> cacheNoOp.get(key, type));
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
            throw new ValueRetrievalException(key, valueLoader, e);
        }
        put(key, value);
        return value;
    }

    @Override
    public void put(@Nonnull Object key, Object value) {
        var keyEntry = keyToKeyEntry(key);
        var executor = executorHolder.getWriteExecutor();
        log.debug(Constants.MARKER, "About to put value with key [{}] to cache [{}]", key, name);
        executor.execute(() -> resilience.execute(() -> putInternal(keyEntry, value), () -> cacheNoOp.put(key, value)));

    }

    @Override
    public void evict(@Nonnull Object key) {
        var keyEntry = keyToKeyEntry(key);
        var executor = executorHolder.getWriteExecutor();
        log.debug(Constants.MARKER, "About to evict value with key [{}] from cache [{}]", key, name);
        executor.execute(() -> resilience.execute(() -> evictInternal(keyEntry), () -> cacheNoOp.evict(key)));
    }

    @Override
    public void clear() {
        var executor = executorHolder.getClearExecutor();
        log.debug(Constants.MARKER, "About to clear cache [{}]", name);
        executor.execute(() -> resilience.execute(() -> store.clear(name), cacheNoOp::clear));
    }

    @Override
    public CompletableFuture<?> retrieve(Object key) {
        return CompletableFuture.runAsync(() -> get(key)).orTimeout(asyncGetTimeout, TimeUnit.SECONDS);
    }

    @Override
    public <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {
        Callable<T> valueCallable = () -> valueLoader.get().get();
        return CompletableFuture.supplyAsync(() -> get(key, valueCallable)).orTimeout(asyncGetWithLoaderTimeout, TimeUnit.SECONDS);
    }

    @Override
    public void evictExpired(int limit) {
        var executor = executorHolder.getWriteExecutor();
        log.debug(Constants.MARKER, "About to evict expired from cache [{}]", name);
        executor.execute(() -> resilience.execute(() -> store.evictExpired(limit, name), () -> cacheNoOp.evict(limit)));
    }

    private ValueWrapper getInternal(KeyEntry keyEntry) {
        CacheEntry data = getCacheEntry(keyEntry);
        if (data == null) return null;

        Object value = deserializeValue(data.value(), keyEntry.type());
        return () -> value;
    }

    private <T> T getInternal(KeyEntry keyEntry, Class<T> type) {
        CacheEntry data = getCacheEntry(keyEntry);
        if (data == null) {
            return null;
        } else {
            return deserializeValue(data.value(), type);
        }
    }

    private void putInternal(KeyEntry keyEntry, Object value) {
        byte[] normalizedKey = normalizeKey(keyEntry);
        Long longKey = generateKey(normalizedKey);

        byte[] serializedValue = serializeValue(value);

        var entry = CacheEntry.builder()
                              .normalizedKey(normalizedKey)
                              .value(serializedValue)
                              .build();

        store.put(longKey, entry, ttlSeconds, name);
    }

    private void evictInternal(KeyEntry keyEntry) {
        byte[] normalizedKey = normalizeKey(keyEntry);
        Long longKey = generateKey(normalizedKey);
        store.remove(longKey, name);
    }

    private Long generateKey(byte[] normalizedKey) {
        return Hashing.murmur3_128()
                      .hashBytes(normalizedKey)
                      .asLong();
    }

    private byte[] normalizeKey(KeyEntry keyEntry) {
        return serializeKey(keyEntry.rawKey());
    }

    private KeyEntry keyToKeyEntry(Object key) {
        if (key instanceof KeyEntry keyEntry) {
            return keyEntry;
        } else if (this.serializerPair != null) {
            return KeyEntry.builder()
                           .rawKey(key)
                           .type(null)
                           .build();
        } else {
            throw new IllegalArgumentException("Provided key is not KeyEntry");
        }
    }

    private CacheEntry getCacheEntry(KeyEntry key) {
        byte[] normalizedKey = normalizeKey(key);
        Long longKey = generateKey(normalizedKey);

        CacheEntry data = store.get(longKey, name);
        if (data == null || !Arrays.equals(data.normalizedKey(), normalizedKey)) return null;
        return data;
    }

    private Object deserializeValue(byte[] value, Type type) {
        if (serializerPair != null) {
            var ser = serializerPair.valueSerializer();
            return ser.deserialize(value);
        } else {
            return generalSerializer.deserialize(value, type);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T deserializeValue(byte[] value, Class<T> type) {
        if (serializerPair != null) {
            var ser = serializerPair.valueSerializer();
            return (T) ser.deserialize(value);
        } else {
            return generalSerializer.deserialize(value, type);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private byte[] serializeValue(Object value) {
        if (serializerPair != null) {
            PgCacheSerializer ser = serializerPair.valueSerializer();
            return ser.serializeValue(value);
        } else {
            return generalSerializer.serialize(value);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private byte[] serializeKey(Object value) {
        if (serializerPair != null) {
            PgCacheSerializer ser = serializerPair.keySerializer();
            return ser.serializeValue(value);
        } else {
            return generalSerializer.serialize(value);
        }
    }
}
