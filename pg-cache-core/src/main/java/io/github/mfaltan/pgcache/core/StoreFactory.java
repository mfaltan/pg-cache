package io.github.mfaltan.pgcache.core;

public interface StoreFactory {

    Store initializeStore(String name, StoreProperties storeProperties);
}
