package io.github.mfaltan.pgcache.core.serializer;

import io.github.mfaltan.pgcache.core.exception.PgCacheSerializerConfigurationException;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PgCacheSerializerConfiguration {

    private final Map<String, PgCacheSerializerPair> serializerPairMap;

    PgCacheSerializerConfiguration(Map<String, PgCacheSerializerPair> serializerPairMap) {
        this.serializerPairMap = serializerPairMap;
    }

    public PgCacheSerializerPair getSerializerPair(String name) {
        var ret = this.serializerPairMap.computeIfAbsent(name, (n) -> this.serializerPairMap.get(null));
        if (ret == null) {
            throw new PgCacheSerializerConfigurationException(name);
        }
        return ret;
    }

    public static PgCachSerializerConfigurationBuilder builder() {
        return new PgCachSerializerConfigurationBuilder();
    }


    public static class PgCachSerializerConfigurationBuilder {
        private final Map<String, PgCacheSerializerPair> serializerPairMap = new HashMap<>();

        PgCachSerializerConfigurationBuilder() {
        }

        public PgCachSerializerConfigurationBuilder serializerConfiguration(@Nullable String name,
                                                                            PgCacheSerializer<?> keySerializer,
                                                                            PgCacheSerializer<?> valueSerializer) {
            var pair = new PgCacheSerializerPair(keySerializer, valueSerializer);
            serializerPairMap.put(name, pair);
            return this;
        }

        public PgCacheSerializerConfiguration build() {
            return new PgCacheSerializerConfiguration(this.serializerPairMap);
        }
    }
}
