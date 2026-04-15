package io.github.mfaltan.pgcache.core;

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
        if (!(o instanceof CacheEntry that)) return false;
        return Arrays.equals(normalizedKey, that.normalizedKey)
                && Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(normalizedKey);
        result = 31 * result + Arrays.hashCode(value);
        return result;
    }

    @Override
    public String toString() {
        return "CacheEntry{" +
                "normalizedKey=" + Arrays.toString(normalizedKey) +
                ", value=" + Arrays.toString(value) +
                '}';
    }
}
