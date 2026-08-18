package com.koolearn.bms.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${bms.jwt.secret}")
    private String secret;

    @Value("${bms.jwt.expire-days:7}")
    private long expireDays;

    @Value("${bms.jwt.strict:false}")
    private boolean strict;

    private static final String DEFAULT_SECRET = "local-evaluation-jwt-secret-change-me-123456789";

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().isEmpty()) {
            secret = DEFAULT_SECRET;
        }
        if (strict && DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException("JWT_SECRET environment variable must be set to a strong random secret in production (bms.jwt.strict=true).");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("bms.jwt.secret must be at least 32 bytes long.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        long expireMillis = expireDays * 24 * 60 * 60 * 1000L;
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireMillis))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
