package com.linksentinel;

import com.linksentinel.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(AppProperties.class)
public class LinkSentinelApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkSentinelApplication.class, args);
    }
}
