package com.rfrade.payop.domain.model;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RetryPolicy {

    private final int maxRetries;
    private final long baseDelayMs;
    private final double backoffMultiplier;
    private final long maxDelayMs;

    @Inject
    public RetryPolicy() {
        this(5, 1000L, 2.0, 30_000L);
    }

    public RetryPolicy(
            int maxRetries, long baseDelayMs, double backoffMultiplier, long maxDelayMs) {
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.maxDelayMs = maxDelayMs;
    }

    public long delayForAttempt(int attempt) {
        long delay = (long) (baseDelayMs * Math.pow(backoffMultiplier, attempt));
        return Math.min(delay, maxDelayMs);
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getBaseDelayMs() {
        return baseDelayMs;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public long getMaxDelayMs() {
        return maxDelayMs;
    }
}
