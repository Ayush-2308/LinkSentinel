package com.linksentinel.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record StatsResponse(
        String code,
        String url,
        long clicks,
        Instant createdAt,
        Instant expiresAt,
        ScreeningResponse screening,
        List<DayCount> clicksByDay,
        List<LabelCount> referrers,
        List<LabelCount> devices) {

    public record DayCount(LocalDate date, long count) {
    }

    public record LabelCount(String label, long count) {
    }
}
