package io.github.mfaltan.pgcache.core.cache;

import io.github.mfaltan.pgcache.core.cache.PgCacheNoOp.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache.ValueRetrievalException;

import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgCacheNoOpTest {

    private static final String NAME = "name";
    private static final Type TYPE = Type.TEMPORARILY;
    private static final Object KEY = "key";

    private final PgCacheNoOp pgCacheNoOp = new PgCacheNoOp(NAME, TYPE);

    @Mock
    private Callable<String> valueLoader;

    @Test
    void should_return_correct_name() {
        //WHEN
        var actual = pgCacheNoOp.getName();

        //THEN
        assertThat(actual).isEqualTo(NAME);
    }

    @Test
    void should_return_self() {
        //WHEN
        var actual = pgCacheNoOp.getNativeCache();

        //THEN
        assertThat(actual).isEqualTo(pgCacheNoOp);
    }

    @Test
    void should_return_null_when_get() {
        //WHEN
        var actual = pgCacheNoOp.get(KEY);

        //THEN
        assertThat(actual).isNull();
    }

    @Test
    void should_return_null_when_get_with_type() {
        //WHEN
        var actual = pgCacheNoOp.get(KEY, KEY.getClass());

        //THEN
        assertThat(actual).isNull();
    }

    @Test
    void should_return_what_loader_returns_when_get_with_loader() throws Exception {
        //GIVEN
        var expected = "expected";
        when(valueLoader.call()).thenReturn(expected);

        //WHEN
        var actual = pgCacheNoOp.get(KEY, valueLoader);

        //THEN
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_throw_correct_exception_when_loader_crashes() throws Exception {
        //GIVEN
        var e = new RuntimeException();
        when(valueLoader.call()).thenThrow(e);

        //WHEN + THEN
        assertThatThrownBy(() -> pgCacheNoOp.get(KEY, valueLoader)).hasCause(e).isInstanceOf(ValueRetrievalException.class);
    }

    @Test
    void should_not_fail_when_put() {
        //WHEN + THEN
        assertThatCode(() -> pgCacheNoOp.put(KEY, "someValue")).doesNotThrowAnyException();
    }

    @Test
    void evict() {
        //WHEN + THEN
        assertThatCode(() -> pgCacheNoOp.evict(KEY)).doesNotThrowAnyException();
    }

    @Test
    void clear() {
        //WHEN + THEN
        assertThatCode(pgCacheNoOp::clear).doesNotThrowAnyException();
    }

    @Test
    void evictExpired() {
        //WHEN + THEN
        assertThatCode(() -> pgCacheNoOp.evictExpired(1)).doesNotThrowAnyException();
    }
}