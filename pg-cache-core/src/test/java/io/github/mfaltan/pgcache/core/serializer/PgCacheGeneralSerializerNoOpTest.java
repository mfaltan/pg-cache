package io.github.mfaltan.pgcache.core.serializer;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PgCacheGeneralSerializerNoOpTest {

    private final PgCacheGeneralSerializerNoOp serializer = new PgCacheGeneralSerializerNoOp();

    @Test
    void should_throw_unsupported_operation_exception_when_serialize() {
        assertThatThrownBy(() -> serializer.serialize("value")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_throw_unsupported_operation_exception_when_deserialize_with_class() {
        assertThatThrownBy(() -> serializer.deserialize("value".getBytes(), String.class)).isInstanceOf(UnsupportedOperationException.class);


    }

    @Test
    void should_throw_unsupported_operation_exception_when_deserialize_with_type_() {
        Type t = null;
        assertThatThrownBy(() -> serializer.deserialize("value".getBytes(), t)).isInstanceOf(UnsupportedOperationException.class);
    }
}