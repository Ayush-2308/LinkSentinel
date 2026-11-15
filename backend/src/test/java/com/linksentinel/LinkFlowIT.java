package com.linksentinel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class LinkFlowIT extends IntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    org.springframework.data.redis.core.StringRedisTemplate redis;

    @Test
    void createRedirectAndStats() throws Exception {
        String token = register();
        String url = "https://example.com/docs";
        String code = create(token, "{\"url\":\"" + url + "\"}");

        mvc.perform(get("/" + code)
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)")
                        .header("Referer", "https://news.example/story"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", url));

        String body = "";
        for (int i = 0; i < 50; i++) {
            body = mvc.perform(get("/api/links/" + code + "/stats").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            if (body.contains("\"clicks\":1")) {
                break;
            }
            Thread.sleep(40);
        }
        assertThat(body).contains("\"clicks\":1");
        assertThat(body).contains("MOBILE");
        assertThat(body).contains("https://news.example/story");
        assertThat(body).contains("\"source\":\"rules\"");
    }

    @Test
    void secondRedirectIsServedFromCache() throws Exception {
        String token = register();
        String url = "https://example.com/cached";
        String code = create(token, "{\"url\":\"" + url + "\"}");

        mvc.perform(get("/" + code)).andExpect(status().isFound()).andExpect(header().string("Location", url));
        assertThat(redis.opsForValue().get("link:" + code)).isNotNull();

        jdbc.update("update links set url = ? where code = ?", "https://changed.example/path", code);
        mvc.perform(get("/" + code)).andExpect(status().isFound()).andExpect(header().string("Location", url));

        redis.delete("link:" + code);
        mvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://changed.example/path"));
    }

    @Test
    void duplicateAliasReturns409() throws Exception {
        String token = register();
        String body = "{\"url\":\"https://example.com/alias\",\"alias\":\"myalias\"}";
        create(token, body);
        mvc.perform(post("/api/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidUrlReturns400() throws Exception {
        String token = register();
        mvc.perform(post("/api/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"notaurl\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expiredLinkReturns410() throws Exception {
        String token = register();
        String expires = Instant.now().plus(Duration.ofHours(1)).toString();
        String code = create(token, "{\"url\":\"https://example.com/old\",\"expiresAt\":\"" + expires + "\"}");
        jdbc.update("update links set expires_at = now() - interval '1 day' where code = ?", code);
        mvc.perform(get("/" + code)).andExpect(status().isGone());
    }

    @Test
    void unauthenticatedApiReturns401() throws Exception {
        mvc.perform(get("/api/links")).andExpect(status().isUnauthorized());
    }

    private String register() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private String create(String token, String json) throws Exception {
        MvcResult result = mvc.perform(post("/api/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.code");
    }
}
