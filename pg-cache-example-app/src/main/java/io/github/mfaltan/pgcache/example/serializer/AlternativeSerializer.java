package io.github.mfaltan.pgcache.example.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializer;
import io.github.mfaltan.pgcache.example.Constants;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class AlternativeSerializer extends PgCacheSerializer {
    public AlternativeSerializer(ObjectMapper mapper) {
        super(mapper);
    }

    @Override
    @Nullable
    public Collection<String> getCacheNames() {
        return List.of(Constants.CACHE_2);
    }
}
