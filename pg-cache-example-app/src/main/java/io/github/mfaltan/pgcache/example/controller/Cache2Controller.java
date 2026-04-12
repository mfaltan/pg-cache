package io.github.mfaltan.pgcache.example.controller;

import io.github.mfaltan.pgcache.example.dto.CacheRequest;
import io.github.mfaltan.pgcache.example.dto.CacheResponse;
import io.github.mfaltan.pgcache.example.service.CacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache/cache2")
@RequiredArgsConstructor
public class Cache2Controller {

    private final CacheService service;

    @PostMapping
    public CacheResponse get(@RequestBody @Valid CacheRequest request) {
        var result = service.getCache(request);
        return new CacheResponse(result);
    }

    @DeleteMapping
    public void evict(@RequestParam("age") int age,
                      @RequestParam("name") String name) {
        var cacheRequest = CacheRequest.builder()
                                       .age(age)
                                       .name(name)
                                       .build();
        service.evictCache(cacheRequest);
    }
}