package com.linksentinel.link;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.linksentinel.config.AppProperties;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class LinkCache {

    private final StringRedisTemplate redis;
    private final Duration ttl;
    private final boolean enabled;

    public LinkCache(StringRedisTemplate redis, AppProperties props) {
        this.redis = redis;
        this.ttl = props.cacheTtl();
        this.enabled = props.cacheEnabled();
    }

    public Optional<Cached> get(String code) {
        if (!enabled) {
            return Optional.empty();
        }
        String value = redis.opsForValue().get(key(code));
        if (value == null) {
            return Optional.empty();
        }
        int split = value.indexOf('\n');
        if (split < 1) {
            return Optional.empty();
        }
        long id = Long.parseLong(value.substring(0, split));
        return Optional.of(new Cached(id, value.substring(split + 1)));
    }

    public void put(Link link) {
        if (!enabled) {
            return;
        }
        Duration lifetime = ttl;
        if (link.getExpiresAt() != null) {
            Duration untilExpiry = Duration.between(Instant.now(), link.getExpiresAt());
            if (untilExpiry.compareTo(lifetime) < 0) {
                lifetime = untilExpiry;
            }
        }
        if (lifetime.isZero() || lifetime.isNegative()) {
            return;
        }
        redis.opsForValue().set(key(link.getCode()), link.getId() + "\n" + link.getUrl(), lifetime);
    }

    public void evict(String code) {
        redis.delete(key(code));
    }

    static String key(String code) {
        return "link:" + code;
    }

    public record Cached(long id, String url) {
    }
}
