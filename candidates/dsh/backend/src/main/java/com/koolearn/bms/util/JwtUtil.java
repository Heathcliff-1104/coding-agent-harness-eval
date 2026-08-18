package com.koolearn.bms.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    private static final String SECRET_STRING = System.getenv().getOrDefault("JWT_SECRET", "local-evaluation-jwt-secret-change-me-123456789");
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    static {
        if ("local-evaluation-jwt-secret-change-me-123456789".equals(SECRET_STRING)) {
            System.err.println("[安全警告] JWT_SECRET 未设置，使用内置默认密钥！生产环境必须通过环境变量 JWT_SECRET 设置强随机密钥。");
        }
    }
    private static final long EXPIRE = 7 * 24 * 60 * 60 * 1000L;

    public static String generate(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
