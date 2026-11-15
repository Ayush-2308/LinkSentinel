package com.linksentinel.click;

import java.util.Locale;

public final class DeviceTypes {

    private DeviceTypes() {
    }

    public static DeviceType fromUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return DeviceType.DESKTOP;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("bot") || ua.contains("spider") || ua.contains("crawler")) {
            return DeviceType.BOT;
        }
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return DeviceType.TABLET;
        }
        if (ua.contains("mobi") || ua.contains("iphone") || ua.contains("android")) {
            return DeviceType.MOBILE;
        }
        return DeviceType.DESKTOP;
    }
}
