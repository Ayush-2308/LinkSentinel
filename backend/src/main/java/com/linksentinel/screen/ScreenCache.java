package com.linksentinel.screen;

import java.time.Duration;
import java.util.Optional;

public interface ScreenCache {

    Optional<ScreeningResult> get(String urlHash);

    void put(String urlHash, ScreeningResult result, Duration ttl);
}
