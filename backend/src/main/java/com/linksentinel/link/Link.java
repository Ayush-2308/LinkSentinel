package com.linksentinel.link;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "links")
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private long ownerId;

    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean screenSafe;

    @Column(nullable = false, length = 500)
    private String screenReason;

    @Column(nullable = false, length = 200)
    private String screenTitle;

    @Column(nullable = false, length = 1000)
    private String screenSummary;

    @Column(nullable = false, length = 16)
    private String screenSource;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isScreenSafe() {
        return screenSafe;
    }

    public void setScreenSafe(boolean screenSafe) {
        this.screenSafe = screenSafe;
    }

    public String getScreenReason() {
        return screenReason;
    }

    public void setScreenReason(String screenReason) {
        this.screenReason = screenReason;
    }

    public String getScreenTitle() {
        return screenTitle;
    }

    public void setScreenTitle(String screenTitle) {
        this.screenTitle = screenTitle;
    }

    public String getScreenSummary() {
        return screenSummary;
    }

    public void setScreenSummary(String screenSummary) {
        this.screenSummary = screenSummary;
    }

    public String getScreenSource() {
        return screenSource;
    }

    public void setScreenSource(String screenSource) {
        this.screenSource = screenSource;
    }
}
