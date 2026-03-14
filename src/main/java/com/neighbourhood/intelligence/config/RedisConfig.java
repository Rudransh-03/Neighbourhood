package com.neighbourhood.intelligence.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Bean
    public RedisClient redisClient(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password:}") String password) {

        RedisURI.Builder builder = RedisURI.builder()
                .withHost(host)
                .withPort(port);

        if (password != null && !password.isBlank()) {
            builder.withPassword(password.toCharArray());
        }

        return RedisClient.create(builder.build());
    }
}