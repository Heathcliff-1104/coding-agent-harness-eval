package com.koolearn.bms.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 60_000;
    private static final long BLOCK_MS = 300_000;

    private final ConcurrentHashMap<String, WindowCounter> store = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        WindowCounter c = store.get(key);
        if (c == null) return false;
        if (c.blockUntil > System.currentTimeMillis()) return true;
        if (System.currentTimeMillis() - c.windowStart > WINDOW_MS) {
            store.remove(key);
            return false;
        }
        return false;
    }

    public void recordAttempt(String key) {
        long now = System.currentTimeMillis();
        WindowCounter c = store.computeIfAbsent(key, k -> new WindowCounter(now));
        if (now - c.windowStart > WINDOW_MS) {
            c.count = 0;
            c.windowStart = now;
        }
        c.count++;
        if (c.count > MAX_ATTEMPTS) {
            c.blockUntil = now + BLOCK_MS;
        }
    }

    private static class WindowCounter {
        long windowStart;
        int count;
        long blockUntil;
        WindowCounter(long start) { this.windowStart = start; this.count = 0; }
    }
}
