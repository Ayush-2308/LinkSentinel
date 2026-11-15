package com.linksentinel.click;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class ClickTracker implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ClickTracker.class);

    private final ClickRepository clicks;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ClickTracker(ClickRepository clicks) {
        this.clicks = clicks;
    }

    public void track(long linkId, String referrer, String userAgent) {
        String storedReferrer = referrer == null || referrer.isBlank() ? null : cut(referrer);
        DeviceType device = DeviceTypes.fromUserAgent(userAgent);
        executor.execute(() -> {
            try {
                Click click = new Click();
                click.setLinkId(linkId);
                click.setReferrer(storedReferrer);
                click.setDeviceType(device);
                clicks.save(click);
            } catch (RuntimeException ex) {
                log.warn("Click was not stored: {}", ex.getMessage());
            }
        });
    }

    @Override
    public void destroy() {
        executor.close();
    }

    private static String cut(String value) {
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
