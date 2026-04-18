package io.github.mfaltan.pgcache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@ComponentScan(basePackages = "io.github.mfaltan.pgcache")
public class TestBootstrap {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules();
    }
}
