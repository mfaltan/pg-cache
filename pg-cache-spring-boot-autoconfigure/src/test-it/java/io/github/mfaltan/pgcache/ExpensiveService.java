package io.github.mfaltan.pgcache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ExpensiveService {

    private final AtomicInteger counter = new AtomicInteger();

    @Cacheable(cacheNames = "cache1")
    public String compute(String input) {
        return "value-" + input + "-" + counter.incrementAndGet();
    }

    @CacheEvict(cacheNames = "cache1")
    public String evict(String input) {
        return "evict-" + input + "-" + counter.incrementAndGet();
    }

    public int getInvocationCount() {
        return counter.get();
    }
}