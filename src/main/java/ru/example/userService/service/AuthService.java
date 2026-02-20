package ru.example.userService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.example.userService.dto.AuthResponse;
import ru.example.userService.dto.LoginRequest;
import ru.example.userService.dto.RefreshRequest;
import ru.example.userService.entity.RefreshToken;
import ru.example.userService.entity.User;
import ru.example.userService.exception.InvalidTokenException;
import ru.example.userService.repository.RefreshTokenRepository;
import ru.example.userService.repository.UserRepository;
import ru.example.userService.security.JwtUtil;

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

        // Бросит BadCredentialsException если неверный пароль — поймает хэндлер
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found after auth"));

        // Отзываем старые refresh-токены пользователя перед выдачей нового
        refreshTokenRepository.revokeAllByUser(user);

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        String refreshTokenRaw = jwtUtil.generateRefreshToken(user.getEmail(), user.getId());

        // Сохраняем хэш refresh-токена в БД
        Instant expiresAt = jwtUtil.getRefreshExpiration();
        RefreshToken refreshToken = new RefreshToken(jwtUtil.hashToken(refreshTokenRaw), user, expiresAt);
        refreshTokenRepository.save(refreshToken);

        log.info("User {} logged in, issued tokens", user.getEmail());
        return new AuthResponse(accessToken, refreshTokenRaw, user.getId(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public String refreshAccessToken(RefreshRequest request) {
        String rawToken = request.getRefreshToken();

        // Шаг 1 — проверяем подпись и срок через JwtUtil
        if (!jwtUtil.validateToken(rawToken)) {
            log.warn("Refresh token JWT validation failed");
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        // Шаг 2 — ищем хэш в БД
        String tokenHash = jwtUtil.hashToken(rawToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found in DB");
                    return new InvalidTokenException("Refresh token not found");
                });

        // Шаг 3 — проверяем не отозван ли
        if (storedToken.isRevoked()) {
            log.warn("Attempt to use revoked refresh token, userId={}", storedToken.getUser().getId());
            // Отзываем все токены — возможна компрометация
            refreshTokenRepository.revokeAllByUser(storedToken.getUser());
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        // Шаг 4 — проверяем срок в БД (дополнительно к JWT)
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh token expired in DB, userId={}", storedToken.getUser().getId());
            throw new InvalidTokenException("Refresh token expired");
        }

        User user = storedToken.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
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

        // Отзываем все токены пользователя — разлогиниваем со всех устройств
        refreshTokenRepository.revokeAllByUser(storedToken.getUser());
        log.info("User {} logged out, all refresh tokens revoked", storedToken.getUser().getEmail());
    }
}