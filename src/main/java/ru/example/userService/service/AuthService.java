package ru.example.userService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import ru.example.userService.dto.AuthResponse;
import ru.example.userService.dto.LoginRequest;
import ru.example.userService.dto.RefreshRequest;
import ru.example.userService.entity.User;
import ru.example.userService.repository.UserRepository;
import ru.example.userService.security.JwtUtil;
import ru.example.userService.security.SecurityConfig;
import org.springframework.security.core.Authentication;

import java.util.Optional;

//логика для логина, создание access, refresh token

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for:{}", request.getEmail());
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            log.warn("Authentication failed for {}: {}", request.getEmail(), e.getMessage());
            throw new IllegalArgumentException("Invalid credentials");
        }
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found after auth"));
        String access = jwtUtil.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        String refresh = jwtUtil.generateRefreshToken(user.getEmail(), user.getId());
        log.info("User {} authenticated, ussue tokens", user.getEmail());
        return new AuthResponse(access, refresh, user.getId(), user.getEmail(), user.getRole().name());
    }

    public String refreshAccessToken(RefreshRequest request) {
        String refresh = request.getRefreashToken();
        if (!jwtUtil.validateToken(refresh)) {
            log.warn("Invalid refresh token");
            throw new IllegalArgumentException("Invalid refresh token");
        }
        var claims = jwtUtil.parseClaims(refresh);
        String email = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        Optional<User> u =  userRepository.findByEmail(email);
        if (u.isEmpty() || u.get().getId().equals(userId)) {
            log.warn("Refresh token user mismatch: tokenUser={}, dbUserPresent={}", userId, u.isPresent());
            throw new IllegalArgumentException("Invalid refresh token");
        }
        String newAccess = jwtUtil.generateAccessToken(email, u.get().getId(), u.get().getRole().name());
        log.info("Issued new access token for userId={}", userId);
        return newAccess;
    }
}
