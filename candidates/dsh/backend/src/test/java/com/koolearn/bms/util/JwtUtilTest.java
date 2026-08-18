package com.koolearn.bms.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void generateAndParseRoundTrip() {
        String token = JwtUtil.generate(42L, "alice", "warehouse");
        Claims claims = JwtUtil.parse(token);
        assertEquals(42L, Long.valueOf(claims.get("userId").toString()));
        assertEquals("alice", claims.get("username"));
        assertEquals("warehouse", claims.get("role"));
    }

    @Test
    void rejectsTamperedToken() {
        String token = JwtUtil.generate(1L, "bob", "engineer");
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThrows(Exception.class, () -> JwtUtil.parse(tampered));
    }
}
