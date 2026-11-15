package com.linksentinel.screen;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ScreeningServiceTest {

    @Test
    void flagsUnsafeVerdictFromTheLlm() {
        ScreeningService service = service(url ->
                new ScreeningResult(false, "phishing kit", "Login", "Asks for a password", "llm"));
        ScreeningResult result = service.screen("https://example.com");
        assertThat(result.safe()).isFalse();
        assertThat(result.source()).isEqualTo("llm");
        assertThat(result.reason()).isEqualTo("phishing kit");
    }

    @Test
    void llmFailureUsesTheRuleChecker() {
        ScreeningService service = service(url -> {
            throw new IllegalStateException("timeout");
        });
        ScreeningResult unsafe = service.screen("http://192.168.1.5/verify-account");
        assertThat(unsafe.safe()).isFalse();
        assertThat(unsafe.source()).isEqualTo("rules");

        ScreeningResult safe = service.screen("https://example.com/docs");
        assertThat(safe.safe()).isTrue();
        assertThat(safe.source()).isEqualTo("rules");
    }

    @Test
    void secondLookUsesTheCachedVerdict() {
        AtomicInteger calls = new AtomicInteger();
        ScreeningService service = service(url -> {
            calls.incrementAndGet();
            return new ScreeningResult(true, "ok", "Example", "Fine", "llm");
        });
        service.screen("https://example.com/docs");
        service.screen("https://example.com/docs");
        assertThat(calls).hasValue(1);
    }

    private static ScreeningService service(LlmClient llm) {
        return new ScreeningService(new MemoryScreenCache(), llm);
    }

    private static final class MemoryScreenCache implements ScreenCache {
        private final Map<String, ScreeningResult> data = new HashMap<>();

        @Override
        public Optional<ScreeningResult> get(String urlHash) {
            return Optional.ofNullable(data.get(urlHash));
        }

        @Override
        public void put(String urlHash, ScreeningResult result, Duration ttl) {
            data.put(urlHash, result);
        }
    }
}
