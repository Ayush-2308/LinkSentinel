package com.linksentinel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.cache-enabled=false")
class RedirectLoadUncachedTest extends IntegrationTestBase {

    @LocalServerPort
    int port;

    @Autowired
    JsonMapper json;

    @Test
    void uncachedRedirects() throws Exception {
        LoadHarness.run(port, "uncached", json);
    }
}
