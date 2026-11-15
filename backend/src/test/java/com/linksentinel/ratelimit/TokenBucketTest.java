package com.linksentinel.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

    @Test
    void fullBucketAllowsAndKeepsTheRemainder() {
        var decision = TokenBucket.consume(5, 0, 0, 5, 1);
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.tokens()).isEqualTo(4);
        assertThat(decision.retryAfterSeconds()).isZero();
    }

    @Test
    void emptyBucketDeniesAndReportsRetry() {
        var decision = TokenBucket.consume(0, 1_000, 1_000, 5, 0.5);
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfterSeconds()).isEqualTo(2);
        assertThat(decision.tokens()).isZero();
    }

    @Test
    void refillStopsAtCapacity() {
        var decision = TokenBucket.consume(0, 0, 10_000, 5, 1);
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.tokens()).isEqualTo(4);
    }

    @Test
    void partialRefillIsKeptWhenTheRequestIsDenied() {
        var decision = TokenBucket.consume(0.2, 5_000, 5_000, 5, 1);
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.tokens()).isEqualTo(0.2);
        assertThat(decision.retryAfterSeconds()).isEqualTo(1);
    }

    @Test
    void rejectsANonPositiveRefill() {
        assertThatThrownBy(() -> TokenBucket.consume(1, 0, 0, 5, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
