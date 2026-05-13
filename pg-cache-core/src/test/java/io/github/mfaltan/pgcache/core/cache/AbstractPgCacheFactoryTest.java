package io.github.mfaltan.pgcache.core.cache;

import io.github.mfaltan.pgcache.common.PgCacheProperties;
import io.github.mfaltan.pgcache.common.PgCacheProperties.CacheProperties;
import io.github.mfaltan.pgcache.core.executor.CacheExecutorHolder;
import io.github.mfaltan.pgcache.core.serializer.PgCacheGeneralSerializer;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerConfiguration;
import io.github.mfaltan.pgcache.core.serializer.PgCacheSerializerPair;
import io.github.mfaltan.pgcache.core.store.PgCacheStore;
import io.github.mfaltan.pgcache.resilience.CacheResilience;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
abstract class AbstractPgCacheFactoryTest {

    private static final String CACHE_NAME = "cn";

    protected AbstractPgCacheFactory factory;

    @Mock
    protected PgCacheStore store;
    @Mock
    protected CacheExecutorHolder executorHolder;
    @Mock
    protected PgCacheGeneralSerializer generalSerializer;
    @Mock
    protected PgCacheSerializerConfiguration serializerConfiguration;
    @Mock
    protected PgCacheProperties properties;
    @Mock
    private CacheResilience cacheResilience;
    @Mock
    private CacheProperties cacheProperties;
    @Mock
    private PgCacheSerializerPair cacheSerializerPair;

    @Test
    void should_return_no_op_cache() {
        //GIVEN
        var spiedFactory = Mockito.spy(factory);
        var expected = new PgCacheNoOp(CACHE_NAME, PgCacheNoOp.Type.PERMANENTLY);
        doReturn(true).when(spiedFactory).isCacheDisabled(CACHE_NAME);

        //WHEN
        var actual = spiedFactory.createCache(CACHE_NAME, cacheResilience);

        //THEN
        verifyNoInteractions(cacheResilience, store);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_return_real_cache() {
        //GIVEN
        var spiedFactory = Mockito.spy(factory);
        var expected = createExpectedCache(CACHE_NAME, store, executorHolder, cacheResilience, generalSerializer, cacheSerializerPair, properties);
        doReturn(false).when(spiedFactory).isCacheDisabled(CACHE_NAME);
        doReturn(cacheSerializerPair).when(spiedFactory).getSerializerConfiguration(CACHE_NAME);

        //WHEN
        var actual = spiedFactory.createCache(CACHE_NAME, cacheResilience);

        //THEN
        verifyNoInteractions(cacheResilience);
        assertThat(actual).isEqualTo(expected);
        verify(store).initCache(CACHE_NAME);
    }

    @Test
    void should_return_false_when_not_disabled() {
        //GIVEN
        when(properties.getCaches()).thenReturn(emptyMap());

        //WHEN
        var actual = factory.isCacheDisabled(CACHE_NAME);

        //THEN
        assertThat(actual).isFalse();
    }

    @Test
    void should_return_true_when_disabled() {
        //GIVEN
        when(properties.getCaches()).thenReturn(Map.of(CACHE_NAME, cacheProperties));
        when(cacheProperties.isDisabled()).thenReturn(true);

        //WHEN
        var actual = factory.isCacheDisabled(CACHE_NAME);

        //THEN
        assertThat(actual).isTrue();
    }

    @Test
    void should_return_null_serializer_pair() {
        //GIVEN
        when(properties.isCustomSerializers()).thenReturn(false);

        //WHEN
        var actual = factory.getSerializerConfiguration(CACHE_NAME);

        //THEN
        assertThat(actual).isNull();
    }

    @Test
    void should_return_serializer_pair() {
        //GIVEN
        when(properties.isCustomSerializers()).thenReturn(true);
        when(serializerConfiguration.getSerializerPair(CACHE_NAME)).thenReturn(cacheSerializerPair);

        //WHEN
        var actual = factory.getSerializerConfiguration(CACHE_NAME);

        //THEN
        assertThat(actual).isEqualTo(cacheSerializerPair);
    }

    abstract PgCacheImpl createExpectedCache(String name,
                                             PgCacheStore store,
                                             CacheExecutorHolder cacheExecutorHolder,
                                             CacheResilience cacheResilience,
                                             PgCacheGeneralSerializer generalSerializer,
                                             PgCacheSerializerPair cacheSerializerPair,
                                             PgCacheProperties properties);
}