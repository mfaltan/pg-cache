package io.github.mfaltan.pgcache.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CacheEntryTest {

    @Test
    void equals_should_compare_array_contents() {
        var a = new CacheEntry(
                new byte[]{1, 2, 3},
                new byte[]{4, 5, 6}
        );

        var b = new CacheEntry(
                new byte[]{1, 2, 3},
                new byte[]{4, 5, 6}
        );

        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_should_fail_when_values_differ() {
        var a = new CacheEntry(
                new byte[]{1, 2, 3},
                new byte[]{4, 5, 6}
        );

        var b = new CacheEntry(
                new byte[]{1, 2, 3},
                new byte[]{9, 9, 9}
        );

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_should_be_reflexive() {
        var a = new CacheEntry(
                new byte[]{1, 2},
                new byte[]{3, 4}
        );

        assertThat(a).isEqualTo(a);
    }

    @Test
    void equals_should_be_symmetric() {
        var a = new CacheEntry(
                new byte[]{7, 8},
                new byte[]{9}
        );

        var b = new CacheEntry(
                new byte[]{7, 8},
                new byte[]{9}
        );

        assertThat(a).isEqualTo(b);
        assertThat(b).isEqualTo(a);
    }

    @Test
    void hash_code_should_be_equal_for_equal_objects() {
        var a = new CacheEntry(
                new byte[]{1, 2, 3},
                new byte[]{4, 5, 6}
        );

        var b = new CacheEntry(
                new byte[]{1, 2, 3},
                new byte[]{4, 5, 6}
        );

        assertThat(a).hasSameHashCodeAs(b);
    }

    @Test
    void to_string_should_contain_values() {
        var entry = new CacheEntry(
                new byte[]{1, 2},
                new byte[]{3, 4}
        );

        assertThat(entry.toString())
                .contains("1", "2", "3", "4");
    }

    @Test
    void equals_should_be_false_for_null_or_different_type() {
        var a = new CacheEntry(
                new byte[]{1},
                new byte[]{2}
        );

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("string");
    }
}