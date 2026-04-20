package io.github.mfaltan.pgcache.core;

import io.github.mfaltan.pgcache.common.StoreProperties;

public interface StoreFactory {

    Store initializeStore(String name, StoreProperties storeProperties);
}
