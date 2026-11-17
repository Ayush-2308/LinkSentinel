package com.linksentinel.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String baseUrl,
        Duration cacheTtl,
        boolean cacheEnabled,
        String jwtSecret,
        Duration jwtTtl,
        String corsOrigin,
        RateLimit rateLimit,
        Llm llm,
        Demo demo) {

    public record RateLimit(int capacity, double refillPerSecond) {
    }

    public record Demo(String email, String password) {
    }

    public record Llm(String provider, String baseUrl, String apiKey, String model, Duration timeout) {
    }
}
