package org.studyplatform.userService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.studyplatform.userService.dto.ChangePasswordRequest;
import org.studyplatform.userService.dto.RegisterRequest;
import org.studyplatform.userService.dto.UpdateProfileRequest;
import org.studyplatform.userService.dto.UserSettingsRequest;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.entity.UserStatus;
import org.studyplatform.userService.exception.EmailAlreadyTakenException;
import org.studyplatform.userService.exception.InvalidUserRoleException;
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

    @Transactional
    public User register(RegisterRequest request) {
        log.info("Attempt to register user with email={}", request.getEmail());
        Optional<User> exists = userRepository.findByEmail(request.getEmail());
        if (exists.isPresent()) {
            log.warn("Registration failed: email already taken: {}", request.getEmail());
            throw new EmailAlreadyTakenException(request.getEmail());
        }

        Role role = resolveSelfServiceRole(request.getRole());

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setFullName(request.getFullName());
        user.setStatus(UserStatus.ACTIVE);
        user.setPreferredLocale("ru");

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

    private Role resolveSelfServiceRole(Role requestedRole) {
        if (requestedRole == null) {
            return Role.STUDENT;
        }
        if (requestedRole == Role.ADMIN) {
            throw new InvalidUserRoleException("ADMIN registration is not allowed through self-service");
        }
        return requestedRole;
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> {
            log.warn("User with id={} not found", id);
            return new UserNotFoundException("User not found");
        });
    }

    @Transactional
    public User updateProfile(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);
        user.setFullName(request.getFullName());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setBio(request.getBio());
        User savedUser = userRepository.save(user);
        log.info("Updated profile for userId={}", savedUser.getId());
        return savedUser;
    }

    @Transactional
    public User updateSettings(String email, UserSettingsRequest request) {
        User user = findByEmail(email);
        user.setFullName(request.getFullName());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setBio(request.getBio());
        user.setPreferredLocale(request.getPreferredLocale());
        User savedUser = userRepository.save(user);
        log.info("Updated settings for userId={}", savedUser.getId());
        return savedUser;
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Password change rejected for userId={}", user.getId());
            throw new BadCredentialsException("Current password is invalid");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Changed password for userId={}", user.getId());
    }
}
