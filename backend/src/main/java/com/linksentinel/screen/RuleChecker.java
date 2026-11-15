package com.linksentinel.screen;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RuleChecker {

    private static final Set<String> BLOCKED_HOSTS = Set.of("phish.test", "malware.example");
    private static final List<String> PATTERNS = List.of(
            "verify-account",
            "verify-your",
            "free-prize",
            "wallet-update",
            "secure-login",
            "account-locked",
            "confirm-password");

    public ScreeningResult check(String url) {
        String host = host(url);
        String lower = url.toLowerCase(Locale.ROOT);
        String reason = null;
        if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || host.contains(":")) {
            reason = "Host is a raw IP address";
        } else if (url.contains("@")) {
            reason = "URL contains embedded credentials";
        } else if (BLOCKED_HOSTS.contains(host)) {
            reason = "Host is on the blocklist";
        } else {
            for (String pattern : PATTERNS) {
                if (lower.contains(pattern)) {
                    reason = "URL matches a suspicious pattern";
                    break;
                }
            }
        }
        if (reason != null) {
            String title = host.isBlank() ? "Suspicious link" : host;
            return new ScreeningResult(false, reason, title, reason, "rules");
        }
        String title = host.isBlank() ? "Link" : host;
        return new ScreeningResult(true, "No suspicious pattern found", title, "Rule check found nothing unusual.", "rules");
    }

    private static String host(String url) {
        int scheme = url.indexOf("://");
        int start = scheme < 0 ? 0 : scheme + 3;
        int end = url.length();
        for (int i = start; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
        }
        String host = url.substring(start, end);
        int at = host.lastIndexOf('@');
        if (at >= 0) {
            host = host.substring(at + 1);
        }
        if (host.startsWith("[")) {
            int close = host.indexOf(']');
            host = close > 0 ? host.substring(1, close) : host;
        } else {
            int colon = host.indexOf(':');
            if (colon >= 0) {
                host = host.substring(0, colon);
            }
        }
        return host.toLowerCase(Locale.ROOT);
    }
}
