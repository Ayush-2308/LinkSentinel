package com.linksentinel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI(AppProperties props) {
        return new OpenAPI()
                .info(new Info()
                        .title("LinkSentinel")
                        .version("1.0")
                        .description("Short links with cached redirects, a Redis token bucket, and link screening. "
                                + "Authenticated routes expect Authorization: Bearer. "
                                + "Over-limit calls return 429 with Retry-After."))
                .addServersItem(new Server().url(props.baseUrl()))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
