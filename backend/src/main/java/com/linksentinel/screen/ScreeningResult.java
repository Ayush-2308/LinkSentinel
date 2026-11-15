package com.linksentinel.screen;

public record ScreeningResult(boolean safe, String reason, String title, String summary, String source) {
}
