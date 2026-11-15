package com.linksentinel.screen;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.linksentinel.config.AppProperties;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(HttpLlmClient.class);
    private static final String SYSTEM = """
            You screen a URL for phishing and spam. Reply with one JSON object and no other text.
            Keys: safe (boolean), reason (short string), title (short page title), summary (one sentence).
            Mark safe false for phishing, malware, scams, or credential harvesting. Mark safe true otherwise.
            """;

    private final RestClient client;
    private final JsonMapper json;
    private final String apiKey;
    private final String model;

    public HttpLlmClient(AppProperties props, JsonMapper json) {
        AppProperties.Llm llm = props.llm();
        this.json = json;
        this.apiKey = llm.apiKey() == null ? "" : llm.apiKey().trim();
        this.model = llm.model();
        Duration timeout = llm.timeout() == null ? Duration.ofSeconds(3) : llm.timeout();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        String base = llm.baseUrl() == null ? "" : llm.baseUrl().replaceAll("/+$", "");
        this.client = RestClient.builder().baseUrl(base).requestFactory(factory).build();
        if (apiKey.isEmpty()) {
            log.info("No LLM API key set; new links will use the rule checker");
        } else {
            log.info("Screening links with {} ({})", llm.provider(), model);
        }
    }

    @Override
    public ScreeningResult screen(String url) {
        if (apiKey.isEmpty()) {
            throw new IllegalStateException("no llm api key");
        }
        String response = client.post()
                .uri("/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "temperature", 0,
                        "messages", List.of(
                                Map.of("role", "system", "content", SYSTEM),
                                Map.of("role", "user", "content", url))))
                .retrieve()
                .body(String.class);
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("empty llm response");
        }
        String content = json.readTree(response).path("choices").path(0).path("message").path("content").asString("");
        if (content.isBlank()) {
            throw new IllegalStateException("empty llm content");
        }
        JsonNode verdict = json.readTree(stripFence(content));
        JsonNode safe = verdict.get("safe");
        if (safe == null || !safe.isBoolean()) {
            throw new IllegalStateException("llm response missing safe");
        }
        return new ScreeningResult(safe.asBoolean(), text(verdict, "reason"), text(verdict, "title"), text(verdict, "summary"), "llm");
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString() || value.asString().isBlank()) {
            throw new IllegalStateException("llm response missing " + field);
        }
        return value.asString().trim();
    }

    private static String stripFence(String content) {
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int breakAt = trimmed.indexOf('\n');
        if (breakAt < 0) {
            return trimmed;
        }
        String body = trimmed.substring(breakAt + 1).trim();
        if (body.endsWith("```")) {
            body = body.substring(0, body.length() - 3).trim();
        }
        return body;
    }
}
