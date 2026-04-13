package io.github.mfaltan.pgcache.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class})
class RamStoreFactoryTest {

    @InjectMocks
    RamStoreFactory ramStoreFactory;

    @Test
    void shouldReturnCorrectStore(){
        // GIVEN
        var expected = new RamStore();

        // WHEN
        var actual = ramStoreFactory.initializeStore("someName");

        // THEN
        assertThat(actual).isEqualTo(expected);
    }
}