package org.studyplatform.userService.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.studyplatform.userService.dto.AuthResponse;
import org.studyplatform.userService.dto.LoginRequest;
import org.studyplatform.userService.dto.RefreshRequest;
import org.studyplatform.userService.entity.RefreshToken;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.exception.InvalidTokenException;
import org.studyplatform.userService.repository.RefreshTokenRepository;
import org.studyplatform.userService.repository.UserRepository;
import org.studyplatform.userService.security.JwtUtil;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_ShouldAuthenticateAndReturnTokenPair() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@test.com");
        request.setPassword("password123");
        User user = user(1L, "student@test.com", Role.STUDENT);

        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(user)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("student@test.com", 1L)).thenReturn("refresh-token");
        when(jwtUtil.hashToken("refresh-token")).thenReturn("refresh-token-hash");
        when(jwtUtil.getRefreshExpiration()).thenReturn(Instant.now().plusSeconds(3600));
        when(jwtUtil.getAccessExpirationSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenRepository).revokeAllByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshAccessToken_ShouldReturnNewAccessToken() {
        RefreshRequest request = refreshRequest("refresh-token");
        User user = user(2L, "teacher@test.com", Role.TEACHER);
        RefreshToken storedToken = new RefreshToken("refresh-token-hash", user, Instant.now().plusSeconds(3600));

        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.hashToken("refresh-token")).thenReturn("refresh-token-hash");
        when(refreshTokenRepository.findByTokenHash("refresh-token-hash")).thenReturn(Optional.of(storedToken));
        when(jwtUtil.generateAccessToken(user)).thenReturn("new-access-token");

        String result = authService.refreshAccessToken(request);

        assertEquals("new-access-token", result);
    }

    @Test
    void refreshAccessToken_ShouldRejectInvalidToken() {
        RefreshRequest request = refreshRequest("invalid-refresh-token");
        when(jwtUtil.validateToken("invalid-refresh-token")).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> authService.refreshAccessToken(request));

        verify(refreshTokenRepository, never()).findByTokenHash(any());
    }

    @Test
    void refreshAccessToken_ShouldRejectRevokedTokenAndRevokeAllUserTokens() {
        RefreshRequest request = refreshRequest("refresh-token");
        User user = user(3L, "admin@test.com", Role.ADMIN);
        RefreshToken storedToken = new RefreshToken("refresh-token-hash", user, Instant.now().plusSeconds(3600));
        storedToken.setRevoked(true);

        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.hashToken("refresh-token")).thenReturn("refresh-token-hash");
        when(refreshTokenRepository.findByTokenHash("refresh-token-hash")).thenReturn(Optional.of(storedToken));

        assertThrows(InvalidTokenException.class, () -> authService.refreshAccessToken(request));

        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    @Test
    void refreshAccessToken_ShouldRejectExpiredToken() {
        RefreshRequest request = refreshRequest("refresh-token");
        User user = user(4L, "student@test.com", Role.STUDENT);
        RefreshToken storedToken = new RefreshToken("refresh-token-hash", user, Instant.now().minusSeconds(1));

        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.hashToken("refresh-token")).thenReturn("refresh-token-hash");
        when(refreshTokenRepository.findByTokenHash("refresh-token-hash")).thenReturn(Optional.of(storedToken));

        assertThrows(InvalidTokenException.class, () -> authService.refreshAccessToken(request));
    }

    @Test
    void logout_ShouldRevokeUserRefreshTokens() {
        User user = user(5L, "student@test.com", Role.STUDENT);
        RefreshToken storedToken = new RefreshToken("refresh-token-hash", user, Instant.now().plusSeconds(3600));

        when(jwtUtil.hashToken("refresh-token")).thenReturn("refresh-token-hash");
        when(refreshTokenRepository.findByTokenHash("refresh-token-hash")).thenReturn(Optional.of(storedToken));

        authService.logout("refresh-token");

        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    @Test
    void logout_ShouldRejectBlankToken() {
        assertThrows(InvalidTokenException.class, () -> authService.logout(" "));
    }

    private RefreshRequest refreshRequest(String token) {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(token);
        return request;
    }

    private User user(Long id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }
}
