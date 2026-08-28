package com.greentraffic.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for metrics adapters.
 */
@Component
@ConfigurationProperties(prefix = "metrics")
public class MetricsProperties {
    private String influxUrl;
    private String vmUrl;
    private String vmQueryUrl;
    private int batchSize = 500;
    /**
     * Maximum in-memory buffer capacity for pending metric points.
     */
    private int bufferCapacity = 10000;
    /**
     * Max retry attempts for HTTP writes
     */
    private int maxRetries = 3;
    /**
     * Initial retry backoff in milliseconds
     */
    private long retryInitialDelayMs = 500L;
    /**
     * Flush interval for background flusher in milliseconds
     */
    private long flushIntervalMs = 2000L;
    /**
     * If true, drop points after exhausting retries instead of requeueing.
     * Useful for development environments to avoid unbounded memory growth.
     */
    private boolean dropOnFailure = false;
    /**
     * Optional local file path to append failed payloads when writes fail.
     */
    private String fallbackFilePath;
    /**
     * Auth type: none | bearer | basic
     */
    private String authType = "none";
    /**
     * Bearer token (if using bearer)
     */
    private String token;
    /**
     * Basic auth username
     */
    private String username;
    /**
     * Basic auth password
     */
    private String password;

    public String getInfluxUrl() {
        return influxUrl;
    }

    public void setInfluxUrl(String influxUrl) {
        this.influxUrl = influxUrl;
    }

    public String getVmUrl() {
        return vmUrl;
    }

    public void setVmUrl(String vmUrl) {
        this.vmUrl = vmUrl;
    }

    public String getVmQueryUrl() {
        return vmQueryUrl;
    }

    public void setVmQueryUrl(String vmQueryUrl) {
        this.vmQueryUrl = vmQueryUrl;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBufferCapacity() {
        return bufferCapacity;
    }

    public void setBufferCapacity(int bufferCapacity) {
        this.bufferCapacity = bufferCapacity;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryInitialDelayMs() {
        return retryInitialDelayMs;
    }

    public void setRetryInitialDelayMs(long retryInitialDelayMs) {
        this.retryInitialDelayMs = retryInitialDelayMs;
    }

    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }

    public boolean isDropOnFailure() {
        return dropOnFailure;
    }

    public void setDropOnFailure(boolean dropOnFailure) {
        this.dropOnFailure = dropOnFailure;
    }

    public String getFallbackFilePath() {
        return fallbackFilePath;
    }

    public void setFallbackFilePath(String fallbackFilePath) {
        this.fallbackFilePath = fallbackFilePath;
    }

    public void setFlushIntervalMs(long flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
    }
}
