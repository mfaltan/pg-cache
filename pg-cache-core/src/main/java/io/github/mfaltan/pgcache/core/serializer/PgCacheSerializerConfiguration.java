package io.github.mfaltan.pgcache.core.serializer;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Builder
@Getter
public class PgCacheSerializerConfiguration {

   private final PgCacheSerializer<?> valueSerializer;
   private final PgCacheSerializer<?> keySerializer;
}
