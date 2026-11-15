package com.linksentinel.ratelimit;

import java.util.List;

import com.linksentinel.config.AppProperties;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimiter {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<String> script;
    private final int capacity;
    private final double refillPerSecond;
    private final String ttlSeconds;

    public RedisRateLimiter(StringRedisTemplate redis, AppProperties props) {
        if (props.rateLimit().capacity() < 1 || props.rateLimit().refillPerSecond() <= 0) {
            throw new IllegalStateException("rate limit capacity and refill must be positive");
        }
        this.redis = redis;
        this.capacity = props.rateLimit().capacity();
        this.refillPerSecond = props.rateLimit().refillPerSecond();
        long ttl = (long) Math.ceil(capacity / refillPerSecond) + 60;
        this.ttlSeconds = Long.toString(Math.max(ttl, 1));
        this.script = new DefaultRedisScript<>();
        this.script.setLocation(new ClassPathResource("redis/token_bucket.lua"));
        this.script.setResultType(String.class);
    }

    public TokenBucket.Decision tryConsume(String bucket) {
        // Capacity is part of the key so a config change does not keep a bucket filled under the old limit.
        String key = "rl:" + capacity + ":" + bucket;
        String raw = redis.execute(
                script,
                List.of(key),
                Integer.toString(capacity),
                Double.toString(refillPerSecond),
                Long.toString(System.currentTimeMillis()),
                ttlSeconds);
        if (raw == null || !raw.contains(":")) {
            throw new IllegalStateException("rate limit script returned nothing");
        }
        String[] parts = raw.split(":");
        boolean allowed = "1".equals(parts[0]);
        long retry = Long.parseLong(parts[1]);
        return new TokenBucket.Decision(allowed, 0, retry);
    }
}
