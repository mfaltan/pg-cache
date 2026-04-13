package io.github.mfaltan.pgcache.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RamStoreTest {

    private static final Long KEY_1 = 1L;
    private static final Long KEY_2 = 2L;

    @InjectMocks
    private RamStore store;

    @Mock
    private CacheEntry entry1, entry2;

    @Test
    void shouldPutAndGetEntry() {
        // GIVEN
        store.put(KEY_1, entry1);

        // WHEN
        var result = store.get(KEY_1);

        // THEN
        assertThat(result).isEqualTo(entry1);
    }

    @Test
    void shouldReturnNullWhenKeyDoesNotExist() {
        // WHEN
        var result = store.get(KEY_1);

        // THEN
        assertThat(result).isNull();
    }

    @Test
    void shouldRemoveEntry() {
        // GIVEN
        store.put(KEY_1, entry1);

        // WHEN
        store.remove(KEY_1);
        var result = store.get(KEY_1);

        // THEN
        assertThat(result).isNull();
    }

    @Test
    void shouldClearAllEntries() {
        // GIVEN
        store.put(KEY_1, entry1);
        store.put(KEY_2, entry2);

        // WHEN
        store.clear();

        // THEN
        assertThat(store.get(KEY_1)).isNull();
        assertThat(store.get(KEY_2)).isNull();
    }
}