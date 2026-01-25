package ru.example.userService.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.example.userService.dto.AuthResponse;
import ru.example.userService.dto.LoginRequest;
import ru.example.userService.dto.RefreshRequest;
import ru.example.userService.dto.RegisterRequest;
import ru.example.userService.entity.User;
import ru.example.userService.service.AuthService;
import ru.example.userService.service.UserService;

import java.util.Map;

//контроллер для аутенфикации (/api/auth/* (register, login, refresh))

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User saved = userService.register(request);
        log.info("Register endpoint: created user id = {}, email = {}", saved.getId(), saved.getEmail());
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        log.info("Login endpoint: success for email = {}", request.getEmail());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        String newAccess = authService.refreshAccessToken(request);
        return ResponseEntity.ok(Map.of("accesstoken", newAccess));
    }
}
