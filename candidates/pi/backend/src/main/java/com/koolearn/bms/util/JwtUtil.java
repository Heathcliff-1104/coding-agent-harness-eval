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

    /** 内置默认密钥（≥32 字节，满足 HS256 要求） */
    private static final String DEFAULT_SECRET = "local-evaluation-jwt-secret-change-me-123456789";

    private static final String SECRET_STRING = resolveSecret();
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRE = 7 * 24 * 60 * 60 * 1000L;

    /**
     * 若设置了 JWT_SECRET 环境变量但长度 < 32 字节（HS256 最低要求），
     * 启动即抛出明确错误，避免运行时 WeakKeyException 排查困难。
     */
    private static String resolveSecret() {
        String env = System.getenv("JWT_SECRET");
        if (env != null && !env.trim().isEmpty()) {
            if (env.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException(
                        "JWT_SECRET 环境变量长度不足 32 字节（HS256 要求密钥 ≥ 32 字节）。"
                                + "请设置更长的 JWT_SECRET，或移除该环境变量以使用内置默认密钥。");
            }
            return env;
        }
        return DEFAULT_SECRET;
    }

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
