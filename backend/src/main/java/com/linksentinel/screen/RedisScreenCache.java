package com.linksentinel.screen;

import java.time.Duration;
import java.util.Optional;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisScreenCache implements ScreenCache {

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public RedisScreenCache(StringRedisTemplate redis, JsonMapper json) {
        this.redis = redis;
        this.json = json;
    }

    @Override
    public Optional<ScreeningResult> get(String urlHash) {
        String value = redis.opsForValue().get(key(urlHash));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(json.readValue(value, ScreeningResult.class));
    }

    @Override
    public void put(String urlHash, ScreeningResult result, Duration ttl) {
        redis.opsForValue().set(key(urlHash), json.writeValueAsString(result), ttl);
    }

    private static String key(String urlHash) {
        return "screen:" + urlHash;
    }
}
