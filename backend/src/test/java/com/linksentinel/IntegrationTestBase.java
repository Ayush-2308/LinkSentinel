package com.linksentinel;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import redis.embedded.RedisServer;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    private static final Services SERVICES = Services.start();

    @DynamicPropertySource
    static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> SERVICES.jdbcUrl);
        registry.add("spring.datasource.username", () -> SERVICES.username);
        registry.add("spring.datasource.password", () -> SERVICES.password);
        registry.add("spring.data.redis.host", () -> SERVICES.redisHost);
        registry.add("spring.data.redis.port", () -> SERVICES.redisPort);
    }

    private static final class Services implements Runnable {
        final String jdbcUrl;
        final String username;
        final String password;
        final String redisHost;
        final int redisPort;
        private final Runnable stop;

        private Services(String jdbcUrl, String username, String password, String redisHost, int redisPort, Runnable stop) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
            this.redisHost = redisHost;
            this.redisPort = redisPort;
            this.stop = stop;
        }

        static Services start() {
            Services services;
            try {
                services = dockerEngineUp() ? testcontainers() : embedded();
            } catch (RuntimeException ex) {
                System.out.println("Testcontainers unavailable (" + ex.getMessage() + "). Starting embedded Postgres and Redis.");
                services = embedded();
            }
            Runtime.getRuntime().addShutdownHook(new Thread(services, "linksentinel-test-services"));
            return services;
        }

        @Override
        public void run() {
            stop.run();
        }
    }

    private static boolean dockerEngineUp() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("windows")) {
            return true;
        }
        return Files.exists(Path.of("\\\\.\\pipe\\docker_engine"));
    }

    @SuppressWarnings("resource")
    private static Services testcontainers() {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:18.7-alpine"));
        GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.10.3-alpine")).withExposedPorts(6379);
        postgres.start();
        redis.start();
        System.out.println("Integration services: Testcontainers Postgres and Redis");
        return new Services(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword(),
                redis.getHost(),
                redis.getMappedPort(6379),
                () -> {
                    redis.stop();
                    postgres.stop();
                });
    }

    private static Services embedded() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
            int redisPort = freePort();
            RedisServer redis = RedisServer.newRedisServer().port(redisPort).setting("bind 127.0.0.1").build();
            redis.start();
            System.out.println("Integration services: embedded Postgres and Redis (Docker engine is not running)");
            return new Services(
                    postgres.getJdbcUrl("postgres", "postgres"),
                    "postgres",
                    "postgres",
                    "127.0.0.1",
                    redisPort,
                    () -> {
                        try {
                            redis.stop();
                        } catch (IOException ignored) {
                            // The test JVM is exiting.
                        }
                        try {
                            postgres.close();
                        } catch (IOException ignored) {
                            // The test JVM is exiting.
                        }
                    });
        } catch (IOException ex) {
            throw new IllegalStateException("Could not start Postgres and Redis", ex);
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
