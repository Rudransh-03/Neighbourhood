package com.neighbourhood.intelligence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableConfigurationProperties
@EnableCaching
public class NeighbourhoodIntelligenceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NeighbourhoodIntelligenceApplication.class, args);
    }
}
