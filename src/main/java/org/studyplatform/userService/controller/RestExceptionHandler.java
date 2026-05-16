package org.studyplatform.userService.controller;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.studyplatform.userService.exception.EmailAlreadyTakenException;
import org.studyplatform.userService.exception.InvalidTokenException;
import org.studyplatform.userService.exception.InvalidUserRoleException;
import org.studyplatform.userService.exception.UserNotFoundException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    //409 ошибка на дублирующий email
    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ResponseEntity<?> handleEmailTaken(EmailAlreadyTakenException e) {
        log.warn("Email conflict: {}", e.getMessage());
        return buildError(HttpStatus.CONFLICT, e.getMessage());
    }

    //404 пользователь не найден
    @ExceptionHandler({UsernameNotFoundException.class, EntityNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<?> handleNotFound(RuntimeException e) {
        log.warn("Not found: {}", e.getMessage());
        return buildError(HttpStatus.NOT_FOUND, e.getMessage());
    }

    //400 ошибка на невалидный email и короткий пароль
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        log.warn("Validation error: {}", message);
        return buildError(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(InvalidUserRoleException.class)
    public ResponseEntity<?> handleInvalidUserRole(InvalidUserRoleException e) {
        log.warn("Invalid registration role: {}", e.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // 401 — невалидный или отозванный токен
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<?> handleInvalidToken(InvalidTokenException e) {
        log.warn("Invalid token: {}", e.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    // 401 — неверные credentials при логине
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException e) {
        log.warn("Bad credentials: {}", e.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    //500 все остальное
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception e) {
        log.error("Internal error: {}", e.getMessage(), e);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "message", message,
                "code", status.value(),
                "timestamp", Instant.now().toString()
        ));
    }
}
