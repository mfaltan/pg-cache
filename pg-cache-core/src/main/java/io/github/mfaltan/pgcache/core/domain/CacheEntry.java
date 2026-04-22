package io.github.mfaltan.pgcache.core.domain;

import jakarta.annotation.Nonnull;
import lombok.Builder;

import java.util.Arrays;

@Builder
public record CacheEntry (

    byte[] normalizedKey,  // original key (serialized if needed)
    byte[] value // serialized value
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheEntry(byte[] key, byte[] value1))) return false;
        return Arrays.equals(normalizedKey, key)
                && Arrays.equals(value, value1);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(normalizedKey);
        result = 31 * result + Arrays.hashCode(value);
        return result;
    }

    @Override
    @Nonnull public String toString() {
        return "CacheEntry{" +
                "normalizedKey=" + Arrays.toString(normalizedKey) +
                ", value=" + Arrays.toString(value) +
                '}';
    }
}
