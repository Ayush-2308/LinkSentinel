package com.linksentinel.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.linksentinel.config.AppProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final java.time.Duration ttl;

    public JwtService(AppProperties props) {
        if (props.jwtSecret() == null || props.jwtSecret().length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(props.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.ttl = props.jwtTtl();
    }

    public String issue(long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(Long.toString(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public long userId(String token) {
        String subject = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
        return Long.parseLong(subject);
    }
}
