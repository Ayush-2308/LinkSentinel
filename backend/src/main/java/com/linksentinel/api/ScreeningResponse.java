package com.linksentinel.api;

public record ScreeningResponse(boolean safe, String reason, String title, String summary, String source) {
}
