package com.linksentinel.ratelimit;

import java.io.IOException;

import com.linksentinel.web.JsonErrors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisRateLimiter limiter;

    public RateLimitFilter(RedisRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        TokenBucket.Decision decision = limiter.tryConsume(bucket(request));
        if (!decision.allowed()) {
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            JsonErrors.write(response, 429, "rate_limited", "Too many requests");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String bucket(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return "u:" + userId;
        }
        return "ip:" + request.getRemoteAddr();
    }
}
