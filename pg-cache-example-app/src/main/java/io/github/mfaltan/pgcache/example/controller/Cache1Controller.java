package io.github.mfaltan.pgcache.example.controller;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.example.dto.CacheRequest;
import io.github.mfaltan.pgcache.example.dto.CacheResponse;
import io.github.mfaltan.pgcache.example.service.CacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache/cache1")
@RequiredArgsConstructor
@Slf4j
public class Cache1Controller {

    private final CacheService service;

    @PostMapping
    public CacheResponse get(@RequestBody @Valid CacheRequest request) {
        log.info(Constants.MARKER, "Cache1 get called with request [{}]", request);
        var result = service.getCache(request.age(), request.name());
        return new CacheResponse(result);
    }

    @DeleteMapping
    public void evict(@RequestParam("age") int age,
                      @RequestParam("name") String name) {
        log.info(Constants.MARKER, "Cache1 evict called for age [{}] and name [{}]", age, name);
        service.evictCache(age, name);
    }
}