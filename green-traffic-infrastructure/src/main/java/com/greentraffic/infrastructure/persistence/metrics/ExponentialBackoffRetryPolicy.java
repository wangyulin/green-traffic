package com.greentraffic.infrastructure.persistence.metrics;

public class ExponentialBackoffRetryPolicy implements RetryPolicy {
    private final int maxRetries;
    private final long initialDelayMs;

    public ExponentialBackoffRetryPolicy(int maxRetries, long initialDelayMs) {
        this.maxRetries = Math.max(0, maxRetries);
        this.initialDelayMs = Math.max(0, initialDelayMs);
    }

    @Override
    public boolean shouldRetry(int attempt) {
        return attempt <= maxRetries;
    }

    @Override
    public long nextDelayMs(int attempt) {
        if (attempt <= 0) return initialDelayMs;
        long d = initialDelayMs << (attempt - 1);
        return d > 0 ? d : Long.MAX_VALUE;
    }
}
