package org.studyplatform.userService.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.User;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
                "",
                "test-key",
                "study-platform-user-service",
                "study-platform",
                15,
                7
        );
    }

    @Test
    void generateAccessToken_ShouldContainRequiredClaimsAndRoles() {
        User user = new User();
        user.setId(123L);
        user.setEmail("student@test.com");
        user.setPassword("encoded-password");
        user.setRole(Role.STUDENT);

        String token = jwtUtil.generateAccessToken(user);
        Claims claims = jwtUtil.parseClaims(token);

        assertEquals("123", claims.getSubject());
        assertEquals("study-platform-user-service", claims.getIssuer());
        assertEquals(List.of("study-platform"), List.copyOf(claims.get("aud", Collection.class)));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getId());
        assertEquals("student@test.com", claims.get("email", String.class));
        assertEquals("student", claims.get("username", String.class));
        assertEquals(List.of("STUDENT"), List.copyOf(claims.get("roles", Collection.class)));
    }

    @Test
    void generateAccessToken_ShouldNotContainSensitiveData() {
        User user = new User();
        user.setId(123L);
        user.setEmail("student@test.com");
        user.setPassword("encoded-password");
        user.setRole(Role.STUDENT);

        Claims claims = jwtUtil.parseClaims(jwtUtil.generateAccessToken(user));

        assertNull(claims.get("password"));
        assertNull(claims.get("refreshToken"));
        assertNull(claims.get("tokenHash"));
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsInvalid() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void hashToken_ShouldBeDeterministicAndNotEqualRawToken() {
        String rawToken = "refresh-token";

        String firstHash = jwtUtil.hashToken(rawToken);
        String secondHash = jwtUtil.hashToken(rawToken);

        assertEquals(firstHash, secondHash);
        assertNotEquals(rawToken, firstHash);
    }

    @Test
    void getJwks_ShouldExposePublicKeyOnly() {
        Map<String, Object> jwks = jwtUtil.getJwks();
        Map<String, Object> key = firstJwk(jwks);

        assertEquals("RSA", key.get("kty"));
        assertEquals("sig", key.get("use"));
        assertEquals("test-key", key.get("kid"));
        assertEquals("RS256", key.get("alg"));
        assertTrue(key.containsKey("n"));
        assertTrue(key.containsKey("e"));
        assertFalse(key.containsKey("d"));
        assertFalse(key.containsKey("p"));
        assertFalse(key.containsKey("q"));
    }

    @Test
    void generatedToken_ShouldBeVerifiedWithJwksPublicKey() throws Exception {
        User user = new User();
        user.setId(321L);
        user.setEmail("teacher@study.com");
        user.setRole(Role.TEACHER);
        String token = jwtUtil.generateAccessToken(user);

        Map<String, Object> jwk = firstJwk(jwtUtil.getJwks());
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode((String) jwk.get("n")));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode((String) jwk.get("e")));
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(modulus, exponent));

        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("321", claims.getSubject());
        assertEquals(List.of("TEACHER"), List.copyOf(claims.get("roles", Collection.class)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstJwk(Map<String, Object> jwks) {
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
        assertNotNull(keys);
        assertFalse(keys.isEmpty());
        return keys.getFirst();
    }
}
