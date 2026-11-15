package com.linksentinel.auth;

import java.util.Locale;

import com.linksentinel.api.AuthRequest;
import com.linksentinel.api.AuthResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtService jwt) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    public AuthResponse register(AuthRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with that email already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(request.password()));
        users.save(user);
        return new AuthResponse(jwt.issue(user.getId()), email);
    }

    public AuthResponse login(AuthRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is wrong"));
        if (!passwords.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is wrong");
        }
        return new AuthResponse(jwt.issue(user.getId()), email);
    }
}
