package org.studyplatform.userService.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.studyplatform.userService.dto.AuthResponse;
import org.studyplatform.userService.dto.LoginRequest;
import org.studyplatform.userService.dto.RefreshRequest;
import org.studyplatform.userService.dto.RegisterRequest;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.service.AuthService;
import org.studyplatform.userService.service.UserService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User savedUser = userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User registered successfully",
                "userId", savedUser.getId()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@Valid @RequestBody RefreshRequest request) {
        String accessToken = authService.refreshAccessToken(request);

        return ResponseEntity.ok(Map.of(
                "message", "Access token refreshed successfully",
                "accessToken", accessToken,
                "tokenType", "Bearer"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
