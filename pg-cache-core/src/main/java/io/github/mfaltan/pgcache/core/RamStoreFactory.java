package io.github.mfaltan.pgcache.core;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RamStoreFactory implements StoreFactory{

    @Override
    public Store initializeStore(String name) {
        log.info("Initializing RAM store {}", name);
        return new RamStore();
    }
}
