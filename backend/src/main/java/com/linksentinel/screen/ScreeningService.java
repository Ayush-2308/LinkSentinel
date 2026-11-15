package com.linksentinel.screen;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ScreeningService {

    private static final Logger log = LoggerFactory.getLogger(ScreeningService.class);
    private static final Duration LLM_TTL = Duration.ofHours(24);
    private static final Duration RULE_TTL = Duration.ofMinutes(10);

    private final ScreenCache cache;
    private final LlmClient llm;
    private final RuleChecker rules = new RuleChecker();

    public ScreeningService(ScreenCache cache, LlmClient llm) {
        this.cache = cache;
        this.llm = llm;
    }

    public ScreeningResult screen(String url) {
        String hash = sha256(url);
        var cached = cache.get(hash);
        if (cached.isPresent()) {
            return cached.get();
        }
        ScreeningResult result;
        try {
            result = clamp(llm.screen(url));
        } catch (RuntimeException ex) {
            log.warn("LLM screening failed, using rules: {}", ex.getMessage());
            result = clamp(rules.check(url));
        }
        // A rules result from an outage should expire sooner than a real model verdict.
        Duration ttl = "llm".equals(result.source()) ? LLM_TTL : RULE_TTL;
        cache.put(hash, result, ttl);
        return result;
    }

    private static ScreeningResult clamp(ScreeningResult result) {
        return new ScreeningResult(
                result.safe(),
                bound(result.reason(), 500, "screened"),
                bound(result.title(), 200, "Link"),
                bound(result.summary(), 1000, ""),
                result.source());
    }

    private static String bound(String value, int max, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
