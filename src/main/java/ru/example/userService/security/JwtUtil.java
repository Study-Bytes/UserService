package ru.example.userService.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;

//jwtUtil создает и валидирует access refresh token

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey key;
    private final long accessMillis;
    private final long refreshMillis;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration-minutes:15}") long accessMinutes,
            @Value("${app.jwt.refresh-expiration-days:7}") long refreshDays
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMillis = accessMinutes * 60_000L;
        this.refreshMillis = refreshDays * 24 * 60 * 60_000L;
    }

    public String generateAccessToken(String email, Long userId, String role) {
        Instant now = Instant.now();
        Date iat = Date.from(now);
        Date exp = Date.from(now.plusMillis(accessMillis));
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(iat)
                .setExpiration(exp)
                .addClaims(Map.of("role", role, "userId", userId))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String email, Long userId) {
        Instant now = Instant.now();
        Date iat = Date.from(now);
        Date exp = Date.from(now.plusMillis(refreshMillis));
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(iat)
                .setExpiration(exp)
                .addClaims(Map.of("userId", userId))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        Jwt<?, ?> parsed = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
        Object payload = parsed.getPayload();
        if (payload instanceof Claims) {
            return (Claims) payload;
        } else {
            throw new IllegalArgumentException("Unexpected JWT payload type: " + payload.getClass());
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims c = parseClaims(token);
            Date exp = c.getExpiration();
            if (exp != null && exp.before(new Date())) {
                log.debug("Token expired at {}", exp);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public Instant getRefreshExpiration() {
        return Instant.now().plusMillis(refreshMillis);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}