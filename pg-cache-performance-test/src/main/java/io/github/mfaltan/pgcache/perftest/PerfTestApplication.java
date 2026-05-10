package io.github.mfaltan.pgcache.perftest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PerfTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerfTestApplication.class, args);
    }
}