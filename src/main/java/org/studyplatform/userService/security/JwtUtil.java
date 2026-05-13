package org.studyplatform.userService.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.User;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final PrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String keyId;
    private final String issuer;
    private final String audience;
    private final long accessMillis;
    private final long refreshMillis;

    public JwtUtil(
            @Value("${app.jwt.private-key:}") String privateKeyPem,
            @Value("${app.jwt.key-id:user-service-rsa-1}") String keyId,
            @Value("${app.jwt.issuer:study-platform-user-service}") String issuer,
            @Value("${app.jwt.audience:study-platform}") String audience,
            @Value("${app.jwt.access-expiration-minutes:15}") long accessMinutes,
            @Value("${app.jwt.refresh-expiration-days:7}") long refreshDays
    ) {
        JwtKeys keys = loadKeys(privateKeyPem);
        this.privateKey = keys.privateKey();
        this.publicKey = keys.publicKey();
        this.keyId = keyId;
        this.issuer = issuer;
        this.audience = audience;
        this.accessMillis = accessMinutes * 60_000L;
        this.refreshMillis = refreshDays * 24 * 60 * 60_000L;
    }

    public String generateAccessToken(User user) {
        return generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
    }

    public String generateAccessToken(String email, Long userId, String role) {
        Instant now = Instant.now();
        Date iat = Date.from(now);
        Date exp = Date.from(now.plusMillis(accessMillis));
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(iat)
                .setExpiration(exp)
                .setId(UUID.randomUUID().toString())
                .setIssuer(issuer)
                .addClaims(Map.of(
                        "aud", List.of(audience),
                        "email", email,
                        "username", usernameFromEmail(email),
                        "roles", List.of(Role.valueOf(role).name())
                ))
                .setHeaderParam("kid", keyId)
                .signWith(privateKey, Jwts.SIG.RS256)
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
                .setHeaderParam("kid", keyId)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        Jwt<?, ?> parsed = Jwts.parser()
                .verifyWith(publicKey)
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

    public long getAccessExpirationSeconds() {
        return accessMillis / 1000L;
    }

    public Map<String, Object> getJwks() {
        return Map.of("keys", List.of(Map.of(
                "kty", "RSA",
                "use", "sig",
                "kid", keyId,
                "alg", "RS256",
                "n", base64Url(publicKey.getModulus().toByteArray()),
                "e", base64Url(publicKey.getPublicExponent().toByteArray())
        )));
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

    private JwtKeys loadKeys(String privateKeyPem) {
        try {
            if (privateKeyPem == null || privateKeyPem.isBlank()) {
                log.warn("app.jwt.private-key is not configured. Generated in-memory RSA key for local development.");
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                var keyPair = generator.generateKeyPair();
                return new JwtKeys(keyPair.getPrivate(), (RSAPublicKey) keyPair.getPublic());
            }

            String normalized = privateKeyPem.replace("\\n", "\n")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(normalized);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey parsedPrivateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) parsedPrivateKey;
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                    rsaPrivateKey.getModulus(),
                    rsaPrivateKey.getPublicExponent()
            );
            return new JwtKeys(parsedPrivateKey, (RSAPublicKey) keyFactory.generatePublic(publicKeySpec));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA JWT private key", e);
        }
    }

    private String usernameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private String base64Url(byte[] value) {
        int offset = 0;
        while (offset < value.length - 1 && value[offset] == 0) {
            offset++;
        }
        byte[] unsigned = java.util.Arrays.copyOfRange(value, offset, value.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(unsigned);
    }

    private record JwtKeys(PrivateKey privateKey, RSAPublicKey publicKey) {
    }
}
