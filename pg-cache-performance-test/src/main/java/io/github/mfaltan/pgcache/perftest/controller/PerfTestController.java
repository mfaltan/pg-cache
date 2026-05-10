package io.github.mfaltan.pgcache.perftest.controller;

import io.github.mfaltan.pgcache.perftest.dto.TestResult;
import io.github.mfaltan.pgcache.perftest.service.CacheBenchmarkService;
import io.github.mfaltan.pgcache.perftest.service.CacheChaosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/perf-test")
@RequiredArgsConstructor
@Slf4j
public class PerfTestController {

    private final CacheChaosService chaosService;
    private final CacheBenchmarkService benchmarkService;

    @GetMapping(path = "/chaos")
    public TestResult testChaos(@RequestParam(name = "usePgCache", defaultValue = "true") boolean usePgCache) {
        return chaosService.executeTest(usePgCache);
    }

    @GetMapping(path = "/benchmark")
    public TestResult testBenchmark(@RequestParam(name = "usePgCache", defaultValue = "true") boolean usePgCache) {
        return benchmarkService.executeBenchmark(usePgCache);
    }
}