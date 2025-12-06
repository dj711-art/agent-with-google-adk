package com.example.agent.util;

import java.util.ArrayDeque;
import java.util.Deque;

public class SimpleRateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    public SimpleRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    /**
     * Try to acquire permission to perform a request.
     * Returns true if allowed, false if rate limit exceeded.
     */
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        long boundary = now - windowMillis;
        while (!timestamps.isEmpty() && timestamps.peekFirst() < boundary) {
            timestamps.removeFirst();
        }
        if (timestamps.size() < maxRequests) {
            timestamps.addLast(now);
            return true;
        }
        return false;
    }
}
