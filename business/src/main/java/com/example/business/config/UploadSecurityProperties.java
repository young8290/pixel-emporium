package com.example.business.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "upload.security")
public class UploadSecurityProperties {
    private long imageMaxBytes = 5L * 1024 * 1024;
    private long videoMaxBytes = 100L * 1024 * 1024;
    private long documentMaxBytes = 20L * 1024 * 1024;
    private long remoteMaxBytes = 20L * 1024 * 1024;
    private int connectTimeoutMillis = 3_000;
    private int readTimeoutMillis = 10_000;
    private int maxRedirects = 3;
    private List<String> remoteAllowedHosts = new ArrayList<>();
    private Map<String, String> allowedTypes = defaultAllowedTypes();

    public long getImageMaxBytes() {
        return imageMaxBytes;
    }

    public void setImageMaxBytes(long imageMaxBytes) {
        this.imageMaxBytes = imageMaxBytes;
    }

    public long getVideoMaxBytes() {
        return videoMaxBytes;
    }

    public void setVideoMaxBytes(long videoMaxBytes) {
        this.videoMaxBytes = videoMaxBytes;
    }

    public long getDocumentMaxBytes() {
        return documentMaxBytes;
    }

    public void setDocumentMaxBytes(long documentMaxBytes) {
        this.documentMaxBytes = documentMaxBytes;
    }

    public long getRemoteMaxBytes() {
        return remoteMaxBytes;
    }

    public void setRemoteMaxBytes(long remoteMaxBytes) {
        this.remoteMaxBytes = remoteMaxBytes;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public int getMaxRedirects() {
        return maxRedirects;
    }

    public void setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
    }

    public List<String> getRemoteAllowedHosts() {
        return List.copyOf(remoteAllowedHosts);
    }

    public void setRemoteAllowedHosts(List<String> remoteAllowedHosts) {
        this.remoteAllowedHosts = remoteAllowedHosts == null ? new ArrayList<>() : new ArrayList<>(remoteAllowedHosts);
    }

    public Map<String, String> getAllowedTypes() {
        return Map.copyOf(allowedTypes);
    }

    public void setAllowedTypes(Map<String, String> allowedTypes) {
        this.allowedTypes = allowedTypes == null ? defaultAllowedTypes() : new LinkedHashMap<>(allowedTypes);
    }

    private static Map<String, String> defaultAllowedTypes() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put(".jpg", "image/jpeg");
        defaults.put(".jpeg", "image/jpeg");
        defaults.put(".png", "image/png");
        defaults.put(".webp", "image/webp");
        defaults.put(".gif", "image/gif");
        defaults.put(".mp4", "video/mp4");
        defaults.put(".pdf", "application/pdf");
        defaults.put(".doc", "application/msword");
        defaults.put(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        return defaults;
    }
}
