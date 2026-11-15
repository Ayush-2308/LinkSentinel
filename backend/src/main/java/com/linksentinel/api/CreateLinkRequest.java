package com.linksentinel.api;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateLinkRequest(
        @NotBlank(message = "URL is required") String url,
        @Pattern(regexp = "^[0-9A-Za-z]{3,32}$", message = "Alias must be 3 to 32 letters or digits") String alias,
        Instant expiresAt) {
}
