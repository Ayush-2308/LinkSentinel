package com.linksentinel.api;

import java.time.Instant;

public record LinkResponse(
        String code,
        String shortUrl,
        String url,
        Instant expiresAt,
        Instant createdAt,
        ScreeningResponse screening) {
}
