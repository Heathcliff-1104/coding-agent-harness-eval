package com.koolearn.bms.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {

    @Test
    void blocksAfterTooManyAttempts() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String key = "attacker";
        for (int i = 0; i < 11; i++) {
            limiter.recordAttempt(key);
        }
        assertTrue(limiter.isBlocked(key));
    }

    @Test
    void notBlockedInitially() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        assertFalse(limiter.isBlocked("nobody"));
    }

    @Test
    void differentUsersIndependent() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < 11; i++) limiter.recordAttempt("userA");
        assertTrue(limiter.isBlocked("userA"));
        assertFalse(limiter.isBlocked("userB"));
    }
}
