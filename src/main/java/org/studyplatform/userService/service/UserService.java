package ru.example.userService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.example.userService.dto.RegisterRequest;
import ru.example.userService.entity.Role;
import ru.example.userService.entity.User;
import ru.example.userService.exception.EmailAlreadyTakenException;
import ru.example.userService.repository.UserRepository;
import ru.example.userService.security.SecurityConfig;

import java.util.Optional;

//регистрация и поиск пользователей

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request) {
        log.info("Attempt to register user with email={}", request.getEmail());
        Optional<User> exists = userRepository.findByEmail(request.getEmail());
        if (exists.isPresent()) {
            log.warn("Reqistration failed: email already taken: {}",  request.getEmail());
            throw new EmailAlreadyTakenException(request.getEmail());
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.STUDENT);
        user.setFullName(request.getFullName());
        User savedUser = userRepository.save(user);
        log.info("User registered id={}, email={}", savedUser.getId(), savedUser.getEmail());
        return savedUser;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("User with email={} not found", email);
            return new IllegalArgumentException("User not found");
        });
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> {
            log.warn("User with id={} not found", id);
            return new IllegalArgumentException("User not found");
        });
    }
}
