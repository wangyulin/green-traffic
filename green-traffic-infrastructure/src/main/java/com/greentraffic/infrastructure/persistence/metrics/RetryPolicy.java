package com.greentraffic.infrastructure.persistence.metrics;

public interface RetryPolicy {
    boolean shouldRetry(int attempt);
    long nextDelayMs(int attempt);
}
