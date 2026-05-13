package org.studyplatform.userService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.studyplatform.userService.dto.RegisterRequest;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.exception.EmailAlreadyTakenException;
import org.studyplatform.userService.exception.UserNotFoundException;
import org.studyplatform.userService.repository.UserRepository;

import java.util.Optional;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

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
            log.warn("Registration failed: email already taken: {}", request.getEmail());
            throw new EmailAlreadyTakenException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.STUDENT);
        user.setFullName(request.getFullName());

        try {
            User savedUser = userRepository.save(user);
            log.info("User registered id={}, email={}", savedUser.getId(), savedUser.getEmail());
            return savedUser;
        } catch (DataIntegrityViolationException e) {
            log.warn("Registration failed on database constraint: {}", request.getEmail());
            throw new EmailAlreadyTakenException(request.getEmail());
        }
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("User with email={} not found", email);
            return new UserNotFoundException("User not found");
        });
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> {
            log.warn("User with id={} not found", id);
            return new UserNotFoundException("User not found");
        });
    }
}
