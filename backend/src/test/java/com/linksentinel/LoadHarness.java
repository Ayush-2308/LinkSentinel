package com.linksentinel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import tools.jackson.databind.json.JsonMapper;

final class LoadHarness {

    private static final int REQUESTS = 400;
    private static final int CONCURRENCY = 25;
    private static final int WARMUP = 30;

    private LoadHarness() {
    }

    static void run(int port, String mode, JsonMapper json) throws Exception {
        HttpClient http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        String email = mode + "-" + UUID.randomUUID() + "@example.com";
        String token = token(http, port, email);
        String code = code(http, port, token, json);
        URI target = URI.create("http://127.0.0.1:" + port + "/" + code);
        HttpRequest request = HttpRequest.newBuilder(target).timeout(Duration.ofSeconds(5)).GET().build();

        for (int i = 0; i < WARMUP; i++) {
            http.send(request, HttpResponse.BodyHandlers.discarding());
        }

        long[] samples = new long[REQUESTS];
        AtomicInteger errors = new AtomicInteger();
        AtomicInteger next = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENCY);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int worker = 0; worker < CONCURRENCY; worker++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        int index;
                        while ((index = next.getAndIncrement()) < REQUESTS) {
                            long began = System.nanoTime();
                            try {
                                HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
                                samples[index] = System.nanoTime() - began;
                                if (response.statusCode() != 302) {
                                    errors.incrementAndGet();
                                }
                            } catch (Exception ex) {
                                errors.incrementAndGet();
                                samples[index] = Long.MAX_VALUE;
                            }
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }
            long began = System.nanoTime();
            start.countDown();
            done.await();
            double seconds = (System.nanoTime() - began) / 1_000_000_000.0;
            Arrays.sort(samples);
            double p50 = samples[percentile(REQUESTS, 0.50)] / 1_000_000.0;
            double p95 = samples[percentile(REQUESTS, 0.95)] / 1_000_000.0;
            double rps = REQUESTS / seconds;
            String line = String.format(
                    Locale.US,
                    "mode=%s requests=%d concurrency=%d p50_ms=%.2f p95_ms=%.2f rps=%.1f errors=%d",
                    mode, REQUESTS, CONCURRENCY, p50, p95, rps, errors.get());
            System.out.println(line);
            Path out = Path.of("target");
            Files.createDirectories(out);
            Files.writeString(out.resolve("load-" + mode + ".txt"), line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            if (errors.get() != 0) {
                throw new AssertionError(line);
            }
        }
    }

    private static int percentile(int count, double rank) {
        return Math.min(count - 1, Math.max(0, (int) Math.ceil(rank * count) - 1));
    }

    private static String token(HttpClient http, int port, String email) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new AssertionError("register failed: " + response.statusCode() + " " + response.body());
        }
        return response.body().split("\"token\":\"")[1].split("\"")[0];
    }

    private static String code(HttpClient http, int port, String token, JsonMapper json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/links"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString("{\"url\":\"https://example.com/load\"}"))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new AssertionError("create failed: " + response.statusCode() + " " + response.body());
        }
        return json.readTree(response.body()).get("code").asString();
    }
}
