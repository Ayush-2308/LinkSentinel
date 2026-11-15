package com.linksentinel.ratelimit;

// Readable spec of the bucket. token_bucket.lua runs the same steps inside Redis.
public final class TokenBucket {

    private TokenBucket() {
    }

    public static Decision consume(
            double tokens, long lastRefillMillis, long nowMillis, double capacity, double refillPerSecond) {
        if (capacity < 1 || refillPerSecond <= 0) {
            throw new IllegalArgumentException("capacity and refill must be positive");
        }
        long elapsed = Math.max(0, nowMillis - lastRefillMillis);
        double available = Math.min(capacity, tokens + (elapsed / 1000.0) * refillPerSecond);
        if (available >= 1.0) {
            return new Decision(true, available - 1.0, 0);
        }
        long retry = (long) Math.ceil((1.0 - available) / refillPerSecond);
        if (retry < 1) {
            retry = 1;
        }
        return new Decision(false, available, retry);
    }

    public record Decision(boolean allowed, double tokens, long retryAfterSeconds) {
    }
}
