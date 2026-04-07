package com.example.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter.
 * Uses a sliding window approach — no external dependencies required.
 */
@Service
public class RateLimitService {

    private record Bucket(int count, Instant windowStart) {}

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Check and record an attempt.
     *
     * @param key        Unique key (e.g. "forgot-password:127.0.0.1" or "otp:user@email.com")
     * @param maxAttempts Maximum allowed attempts within the window
     * @param windowSeconds Duration of the sliding window in seconds
     * @return true if the attempt is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String key, int maxAttempts, long windowSeconds) {
        Instant now = Instant.now();

        Bucket current = buckets.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.windowStart().plusSeconds(windowSeconds))) {
                // New window
                return new Bucket(1, now);
            }
            // Same window — increment
            return new Bucket(existing.count() + 1, existing.windowStart());
        });

        return current.count() <= maxAttempts;
    }

    /** Convenience — check only, don't count the attempt. */
    public boolean isBlocked(String key, int maxAttempts, long windowSeconds) {
        Bucket existing = buckets.get(key);
        if (existing == null) return false;
        Instant now = Instant.now();
        if (now.isAfter(existing.windowStart().plusSeconds(windowSeconds))) return false;
        return existing.count() >= maxAttempts;
    }
}