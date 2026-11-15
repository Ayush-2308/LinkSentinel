package com.linksentinel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedirectLoadCachedTest extends IntegrationTestBase {

    @LocalServerPort
    int port;

    @Autowired
    JsonMapper json;

    @Test
    void cachedRedirects() throws Exception {
        LoadHarness.run(port, "cached", json);
    }
}
