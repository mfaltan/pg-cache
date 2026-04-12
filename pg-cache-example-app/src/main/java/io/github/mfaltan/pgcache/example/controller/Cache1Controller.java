package io.github.mfaltan.pgcache.example.controller;

import io.github.mfaltan.pgcache.example.dto.CacheRequest;
import io.github.mfaltan.pgcache.example.dto.CacheResponse;
import io.github.mfaltan.pgcache.example.service.CacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache/cache1")
@RequiredArgsConstructor
public class Cache1Controller {

    private final CacheService service;

    @PostMapping
    public CacheResponse get(@RequestBody @Valid CacheRequest request) {
        var result = service.getCache(request.age(), request.name());
        return new CacheResponse(result);
    }

    @DeleteMapping
    public void evict(@RequestParam("age") int age,
                      @RequestParam("name") String name) {
        service.evictCache(age, name);
    }
}