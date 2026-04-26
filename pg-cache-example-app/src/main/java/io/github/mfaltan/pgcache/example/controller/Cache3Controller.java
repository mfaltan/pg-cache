package io.github.mfaltan.pgcache.example.controller;

import io.github.mfaltan.pgcache.common.Constants;
import io.github.mfaltan.pgcache.example.dto.CacheRequest;
import io.github.mfaltan.pgcache.example.dto.CacheResponse;
import io.github.mfaltan.pgcache.example.service.CacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cache/cache3")
@RequiredArgsConstructor
@Slf4j
public class Cache3Controller {

    private final CacheService service;

    @PostMapping
    public CacheResponse get(@RequestBody @Valid CacheRequest request) {
        log.info(Constants.MARKER, "Cache3 get called with request [{}]", request);
        var result = service.getCache(request.age(), request.age2());
        return new CacheResponse(result);
    }

    @DeleteMapping
    public void evict(@RequestParam("age") int age,
                      @RequestParam("age2") int age2) {
        log.info(Constants.MARKER, "Cache3 evict called with age [{}] and age2 [{}]", age, age2);
        service.evictCache(age, age2);
    }
}