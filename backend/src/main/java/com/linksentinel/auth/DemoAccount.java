package com.linksentinel.auth;

import java.util.Locale;

import com.linksentinel.api.AuthRequest;
import com.linksentinel.config.AppProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoAccount implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccount.class);

    private final AppProperties props;
    private final UserRepository users;
    private final AuthService auth;

    public DemoAccount(AppProperties props, UserRepository users, AuthService auth) {
        this.props = props;
        this.users = users;
        this.auth = auth;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = props.demo().email() == null ? "" : props.demo().email().trim().toLowerCase(Locale.ROOT);
        String password = props.demo().password() == null ? "" : props.demo().password();
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (users.existsByEmail(email)) {
            log.info("Demo account already exists: {}", email);
            return;
        }
        auth.register(new AuthRequest(email, password));
        log.info("Demo account created: {}", email);
    }
}
