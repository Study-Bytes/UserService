package org.studyplatform.userService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.studyplatform.userService.dto.AuthResponse;
import org.studyplatform.userService.dto.LoginRequest;
import org.studyplatform.userService.dto.RefreshRequest;
import org.studyplatform.userService.entity.RefreshToken;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.exception.InvalidTokenException;
import org.studyplatform.userService.repository.RefreshTokenRepository;
import org.studyplatform.userService.repository.UserRepository;
import org.studyplatform.userService.security.JwtUtil;

import java.time.Instant;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found after auth"));
        refreshTokenRepository.revokeAllByUser(user);
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshTokenRaw = jwtUtil.generateRefreshToken(user.getEmail(), user.getId());

        Instant expiresAt = jwtUtil.getRefreshExpiration();
        RefreshToken refreshToken = new RefreshToken(jwtUtil.hashToken(refreshTokenRaw), user, expiresAt);
        refreshTokenRepository.save(refreshToken);

        log.info("User {} logged in, issued tokens", user.getEmail());
        return new AuthResponse(accessToken, refreshTokenRaw, "Bearer", jwtUtil.getAccessExpirationSeconds());
    }

    @Transactional
    public String refreshAccessToken(RefreshRequest request) {
        String rawToken = request.getRefreshToken();
        if (!jwtUtil.validateToken(rawToken)) {
            log.warn("Refresh token JWT validation failed");
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
        String tokenHash = jwtUtil.hashToken(rawToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found in DB");
                    return new InvalidTokenException("Refresh token not found");
                });
        if (storedToken.isRevoked()) {
            log.warn("Attempt to use revoked refresh token, userId={}", storedToken.getUser().getId());
            refreshTokenRepository.revokeAllByUser(storedToken.getUser());
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh token expired in DB, userId={}", storedToken.getUser().getId());
            throw new InvalidTokenException("Refresh token expired");
        }
        User user = storedToken.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user);
        log.info("Issued new access token for userId={}", user.getId());
        return newAccessToken;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required for logout");
        }
        String tokenHash = jwtUtil.hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));
        refreshTokenRepository.revokeAllByUser(storedToken.getUser());
        log.info("User {} logged out, all refresh tokens revoked", storedToken.getUser().getEmail());
    }
}
